package com.upss.middleware;

import com.upss.core.LightweightAuditor;
import com.upss.core.Prompt;
import com.upss.middleware.impl.AuditorMiddleware;
import com.upss.middleware.impl.SanitizerMiddleware;
import com.upss.middleware.impl.ValidatorMiddleware;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

import static org.junit.Assert.*;

public class SecurityPipelineIntegrationTest {

    private SecurityPipeline pipeline;
    private Prompt testPrompt;
    private Prompt criticalPrompt;
    private String testAuditDir;

    @Before
    public void setUp() {
        testAuditDir = System.getProperty("java.io.tmpdir") + "/upss_integration_" + System.currentTimeMillis();
        new File(testAuditDir).mkdirs();
        
        testPrompt = new Prompt("test-prompt", "Test content", "1.0.0", "medium");
        criticalPrompt = new Prompt("critical-prompt", "Critical content", "1.0.0", "critical");
        pipeline = new SecurityPipeline();
    }

    @After
    public void tearDown() {
        File dir = new File(testAuditDir);
        if (dir.exists()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    file.delete();
                }
            }
            dir.delete();
        }
    }

    @Test
    public void testValidatorThenSanitizer() {
        pipeline.use(new ValidatorMiddleware())
                .use(new SanitizerMiddleware());
        
        PipelineResult result = pipeline.execute(testPrompt, "Safe user input");
        assertTrue("Safe input should pass both validators", result.isPassed());
    }

    @Test
    public void testValidatorBlocksOversizedInput() {
        ValidatorMiddleware limitedValidator = new ValidatorMiddleware(50);
        pipeline.use(limitedValidator)
                .use(new SanitizerMiddleware());
        
        String largeInput = "x".repeat(100);
        PipelineResult result = pipeline.execute(testPrompt, largeInput);
        
        assertFalse("Oversized input should fail validation", result.isPassed());
        assertTrue("Error should be from validator", result.getLastError().contains("exceeds maximum"));
    }

    @Test
    public void testSanitizerBlocksAfterValidator() {
        pipeline.use(new ValidatorMiddleware())
                .use(new SanitizerMiddleware());
        
        PipelineResult result = pipeline.execute(testPrompt, "SELECT * FROM users");
        
        assertFalse("Injection should be blocked", result.isPassed());
        assertTrue("Error should mention injection", result.getLastError().contains("injection"));
    }

    @Test
    public void testAllThreeMiddlewareWithCleanInput() {
        LightweightAuditor auditor = new LightweightAuditor(testAuditDir);
        pipeline.use(new ValidatorMiddleware())
                .use(new SanitizerMiddleware())
                .use(new AuditorMiddleware(auditor));
        
        pipeline.withContext("user", "testUser")
                .withContext("sessionId", "sess-123");
        
        PipelineResult result = pipeline.execute(testPrompt, "Safe user input");
        
        assertTrue("Clean input should pass all middleware", result.isPassed());
        File logFile = new File(testAuditDir + "/access.log");
        assertTrue("Audit log should be created", logFile.exists());
    }

    @Test
    public void testMiddlewareOrderingMatters() {
        pipeline.use(new SanitizerMiddleware())
                .use(new ValidatorMiddleware(10));
        
        PipelineResult result = pipeline.execute(testPrompt, "SELECT * FROM ...");
        assertFalse("Should fail sanitizer first", result.isPassed());
    }

    @Test
    public void testCriticalPromptPathTraversalDetection() {
        pipeline.use(new ValidatorMiddleware());
        
        PipelineResult result = pipeline.execute(criticalPrompt, "../../etc/passwd");
        
        assertFalse("Path traversal in critical prompt should fail", result.isPassed());
    }

    @Test
    public void testContextPropagationThroughPipeline() {
        pipeline.withContext("user", "admin")
                .withContext("sessionId", "sess-456")
                .withContext("ip", "192.168.1.1")
                .use(new ValidatorMiddleware());
        
        PipelineResult result = pipeline.execute(testPrompt, "Safe input");
        
        assertEquals("User context should propagate", "admin", result.getContext("user"));
        assertEquals("Session context should propagate", "sess-456", result.getContext("sessionId"));
        assertEquals("Custom context should propagate", "192.168.1.1", result.getContext("ip"));
    }

    @Test
    public void testMultipleSuspiciousPatternsBlocked() {
        pipeline.use(new ValidatorMiddleware())
                .use(new SanitizerMiddleware());
        
        String malicious = "SELECT * FROM users; DROP TABLE accounts; INSERT INTO admin VALUES";
        PipelineResult result = pipeline.execute(testPrompt, malicious);
        
        assertFalse("Multiple attack patterns should be blocked", result.isPassed());
    }

    @Test
    public void testAuditLogsFailureReasons() throws Exception {
        LightweightAuditor auditor = new LightweightAuditor(testAuditDir);
        pipeline.use(new ValidatorMiddleware())
                .use(new SanitizerMiddleware())
                .use(new AuditorMiddleware(auditor))
                .withContext("user", "attacker");
        
        PipelineResult result = pipeline.execute(testPrompt, "DROP TABLE users");
        
        assertFalse("Should fail security checks", result.isPassed());
        
        File securityLogFile = new File(testAuditDir + "/security.log");
        assertTrue("Security event should be logged", securityLogFile.exists());
    }

    @Test
    public void testEmptyPipelineExecutesWithoutMiddleware() {
        PipelineResult result = pipeline.execute(testPrompt, "Anything goes");
        assertTrue("Empty pipeline should always pass", result.isPassed());
    }

    @Test
    public void testPipelineWithOnlyValidator() {
        pipeline.use(new ValidatorMiddleware());
        
        PipelineResult result = pipeline.execute(testPrompt, "Safe input");
        assertTrue("Safe input should pass validator only", result.isPassed());
    }

    @Test
    public void testPipelineWithOnlySanitizer() {
        pipeline.use(new SanitizerMiddleware());
        
        PipelineResult result = pipeline.execute(testPrompt, "SELECT * FROM users");
        assertFalse("Injection should fail sanitizer", result.isPassed());
    }

    @Test
    public void testErrorsAccumulateInPipeline() {
        pipeline.use(new ValidatorMiddleware())
                .use(new SanitizerMiddleware());
        
        PipelineResult result = pipeline.execute(testPrompt, "SELECT * FROM users");
        
        assertFalse("Should report at least one error", result.isPassed());
    }

    @Test
    public void testContextModificationDoesNotAffectNewExecutions() {
        pipeline.withContext("user", "admin");
        
        PipelineResult result1 = pipeline.execute(testPrompt, "input1");
        assertEquals("First execution should have admin user", "admin", result1.getContext("user"));
        
        pipeline.withContext("user", "attacker");
        
        PipelineResult result2 = pipeline.execute(testPrompt, "input2");
        assertEquals("Second execution should have attacker user", "attacker", result2.getContext("user"));
    }

    @Test
    public void testDifferentPromptsWithSamePipeline() {
        pipeline.use(new ValidatorMiddleware());
        
        Prompt prompt1 = new Prompt("p1", "content1", "1.0", "low");
        Prompt prompt2 = new Prompt("p2", "content2", "1.0", "high");
        
        PipelineResult result1 = pipeline.execute(prompt1, "input");
        PipelineResult result2 = pipeline.execute(prompt2, "input");
        
        assertEquals("Result1 should have prompt1", prompt1, result1.getPrompt());
        assertEquals("Result2 should have prompt2", prompt2, result2.getPrompt());
    }

    @Test
    public void testValidatorWithDifferentLimits() {
        ValidatorMiddleware validator1 = new ValidatorMiddleware(100);
        ValidatorMiddleware validator2 = new ValidatorMiddleware(50);
        
        String input = "x".repeat(75);
        
        SecurityPipeline pipeline1 = new SecurityPipeline().use(validator1);
        SecurityPipeline pipeline2 = new SecurityPipeline().use(validator2);
        
        PipelineResult result1 = pipeline1.execute(testPrompt, input);
        PipelineResult result2 = pipeline2.execute(testPrompt, input);
        
        assertTrue("Input should pass 100-char limit", result1.isPassed());
        assertFalse("Input should fail 50-char limit", result2.isPassed());
    }

    @Test
    public void testSanitizationPreservesCleanContent() {
        pipeline.use(new SanitizerMiddleware());
        
        String cleanInput = "Please help me understand machine learning";
        PipelineResult result = pipeline.execute(testPrompt, cleanInput);
        
        assertTrue("Should pass sanitization", result.isPassed());
        assertEquals("Clean content should be preserved", cleanInput, result.getSanitizedInput());
    }

    @Test
    public void testChainedContextAddition() {
        pipeline.withContext("key1", "value1")
                .withContext("key2", "value2")
                .withContext("key3", "value3")
                .use(new ValidatorMiddleware());
        
        PipelineResult result = pipeline.execute(testPrompt, "input");
        
        assertEquals("All context should be present", "value1", result.getContext("key1"));
        assertEquals("All context should be present", "value2", result.getContext("key2"));
        assertEquals("All context should be present", "value3", result.getContext("key3"));
    }

    @Test
    public void testComplexInjectionAttemptBlocked() {
        pipeline.use(new ValidatorMiddleware())
                .use(new SanitizerMiddleware());
        
        String complexAttack = "1'; DROP TABLE users WHERE '1'='1";
        PipelineResult result = pipeline.execute(testPrompt, complexAttack);
        
        assertFalse("Complex injection should be blocked", result.isPassed());
    }

    @Test
    public void testShellCommandInjectionBlocked() {
        pipeline.use(new SanitizerMiddleware());
        
        String shellCommand = "test.txt; rm -rf /";
        PipelineResult result = pipeline.execute(testPrompt, shellCommand);
        
        assertFalse("Shell command injection should be blocked", result.isPassed());
    }

    @Test
    public void testValidationStopsOrallowsExecution() {
        ValidatorMiddleware shortValidator = new ValidatorMiddleware(10);
        pipeline.use(shortValidator)
                .use(new SanitizerMiddleware());
        
        String shortInput = "short";
        String longInput = "this is a very long input";
        
        PipelineResult shortResult = pipeline.execute(testPrompt, shortInput);
        PipelineResult longResult = pipeline.execute(testPrompt, longInput);
        
        assertTrue("Short input should pass", shortResult.isPassed());
        assertFalse("Long input should fail at validation", longResult.isPassed());
    }

    @Test
    public void testPromptIdAccessibleInResult() {
        pipeline.use(new ValidatorMiddleware());
        
        PipelineResult result = pipeline.execute(testPrompt, "input");
        
        assertEquals("Result should contain prompt ID", "test-prompt", result.getPrompt().getId());
    }

    @Test
    public void testUserInputAccessibleInResult() {
        pipeline.use(new ValidatorMiddleware());
        
        String userInput = "user provided text";
        PipelineResult result = pipeline.execute(testPrompt, userInput);
        
        assertEquals("Result should preserve user input", userInput, result.getUserInput());
    }
}
