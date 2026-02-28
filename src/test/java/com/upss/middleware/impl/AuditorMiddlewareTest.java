package com.upss.middleware.impl;

import com.upss.core.LightweightAuditor;
import com.upss.core.Prompt;
import com.upss.middleware.PipelineResult;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.Assert.*;

public class AuditorMiddlewareTest {

    private AuditorMiddleware auditor;
    private LightweightAuditor lightweightAuditor;
    private PipelineResult result;
    private Prompt testPrompt;
    private String testAuditDir;

    @Before
    public void setUp() {
        testAuditDir = System.getProperty("java.io.tmpdir") + "/upss_audit_test_" + System.currentTimeMillis();
        new File(testAuditDir).mkdirs();
        
        lightweightAuditor = new LightweightAuditor(testAuditDir);
        auditor = new AuditorMiddleware(lightweightAuditor);
        testPrompt = new Prompt("test-prompt", "Test content", "1.0.0", "medium");
    }

    @After
    public void tearDown() {
        // Clean up test files
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
    public void testAuditorCreationWithDefault() {
        AuditorMiddleware defaultAuditor = new AuditorMiddleware();
        assertNotNull("Default auditor should be created", defaultAuditor);
        assertNotNull("Default auditor should have internal auditor", defaultAuditor.getAuditor());
    }

    @Test
    public void testAuditorCreationWithCustomAuditor() {
        assertNotNull("Auditor should be created", auditor);
        assertEquals("Should use provided auditor", lightweightAuditor, auditor.getAuditor());
    }

    @Test
    public void testSuccessfulExecutionAudit() {
        result = new PipelineResult(testPrompt, "test input");
        result.addContext("user", "testUser");
        result.addContext("sessionId", "sess-123");
        
        auditor.process(result);
        
        File logFile = new File(testAuditDir + "/access.log");
        assertTrue("Access log should be created", logFile.exists());
    }

    @Test
    public void testFailedExecutionAudit() {
        result = new PipelineResult(testPrompt, "test input");
        result.addContext("user", "testUser");
        result.fail("Security violation");
        
        auditor.process(result);
        
        File securityLogFile = new File(testAuditDir + "/security.log");
        assertTrue("Security log should be created for failures", securityLogFile.exists());
    }

    @Test
    public void testAuditLogsCorrectUser() throws Exception {
        result = new PipelineResult(testPrompt, "test input");
        result.addContext("user", "admin");
        result.addContext("sessionId", "sess-456");
        
        auditor.process(result);
        
        File logFile = new File(testAuditDir + "/access.log");
        assertTrue("Log file should exist", logFile.exists());
        
        List<String> lines = Files.readAllLines(logFile.toPath());
        assertFalse("Log should have entries", lines.isEmpty());
        assertTrue("Log should contain user", lines.get(0).contains("admin"));
    }

    @Test
    public void testAuditLogsSessionId() throws Exception {
        result = new PipelineResult(testPrompt, "test input");
        result.addContext("user", "testUser");
        result.addContext("sessionId", "sess-789");
        
        auditor.process(result);
        
        File logFile = new File(testAuditDir + "/access.log");
        List<String> lines = Files.readAllLines(logFile.toPath());
        assertFalse("Log should have entries", lines.isEmpty());
        assertTrue("Log should contain session ID", lines.get(0).contains("sess-789"));
    }

    @Test
    public void testAuditWithMissingUser() throws Exception {
        result = new PipelineResult(testPrompt, "test input");
        result.addContext("sessionId", "sess-123");
        
        auditor.process(result);
        
        File logFile = new File(testAuditDir + "/access.log");
        List<String> lines = Files.readAllLines(logFile.toPath());
        assertTrue("Log should have fallback for missing user", lines.get(0).contains("unknown"));
    }

    @Test
    public void testAuditWithMissingSessionId() throws Exception {
        result = new PipelineResult(testPrompt, "test input");
        result.addContext("user", "testUser");
       
        auditor.process(result);
        
        File logFile = new File(testAuditDir + "/access.log");
        List<String> lines = Files.readAllLines(logFile.toPath());
        assertTrue("Log should have fallback for missing session", lines.get(0).contains("no-session"));
    }

    @Test
    public void testSecurityEventLogging() throws Exception {
        result = new PipelineResult(testPrompt, "test input");
        result.addContext("user", "attacker");
        result.fail("Injection pattern detected");
        
        auditor.process(result);
        
        File securityLogFile = new File(testAuditDir + "/security.log");
        assertTrue("Security event log should be created", securityLogFile.exists());
        
        List<String> lines = Files.readAllLines(securityLogFile.toPath());
        assertFalse("Security log should have entries", lines.isEmpty());
    }

    @Test
    public void testAuditLogsPromptId() throws Exception {
        Prompt customPrompt = new Prompt("custom-id-123", "Custom content", "1.0.0", "medium");
        result = new PipelineResult(customPrompt, "test input");
        result.addContext("user", "testUser");
        
        auditor.process(result);
        
        File logFile = new File(testAuditDir + "/access.log");
        List<String> lines = Files.readAllLines(logFile.toPath());
        assertTrue("Log should contain prompt ID", lines.get(0).contains("custom-id-123"));
    }

    @Test
    public void testGetName() {
        assertEquals("Name should be AuditorMiddleware", "AuditorMiddleware", auditor.getName());
    }

    @Test
    public void testGetAuditor() {
        assertEquals("Should return internal auditor", lightweightAuditor, auditor.getAuditor());
    }

    @Test
    public void testMultipleAuditCalls() throws Exception {
        result = new PipelineResult(testPrompt, "test input 1");
        result.addContext("user", "user1");
        auditor.process(result);
        
        PipelineResult result2 = new PipelineResult(testPrompt, "test input 2");
        result2.addContext("user", "user2");
        auditor.process(result2);
        
        File logFile = new File(testAuditDir + "/access.log");
        List<String> lines = Files.readAllLines(logFile.toPath());
        assertEquals("Should have two log entries", 2, lines.size());
    }

    @Test
    public void testAuditPassedStatus() throws Exception {
        result = new PipelineResult(testPrompt, "test input");
        result.addContext("user", "testUser");
        auditor.process(result);
        
        File logFile = new File(testAuditDir + "/access.log");
        List<String> lines = Files.readAllLines(logFile.toPath());
        assertTrue("Log should contain PASSED status", lines.get(0).contains("PASSED"));
    }

    @Test
    public void testAuditFailedStatus() throws Exception {
        result = new PipelineResult(testPrompt, "test input");
        result.addContext("user", "testUser");
        result.fail("Test failure");
        auditor.process(result);
        
        File logFile = new File(testAuditDir + "/access.log");
        List<String> lines = Files.readAllLines(logFile.toPath());
        assertTrue("Log should contain FAILED status", lines.get(0).contains("FAILED"));
    }

    @Test
    public void testSecurityEventContainsErrorMessage() throws Exception {
        String errorMsg = "Dangerous SQL injection detected";
        result = new PipelineResult(testPrompt, "test input");
        result.addContext("user", "testUser");
        result.fail(errorMsg);
        auditor.process(result);
        
        File securityLogFile = new File(testAuditDir + "/security.log");
        List<String> lines = Files.readAllLines(securityLogFile.toPath());
        assertTrue("Security log should contain error message", lines.get(0).contains(errorMsg));
    }

    @Test
    public void testAuditActionIsExecute() throws Exception {
        result = new PipelineResult(testPrompt, "test input");
        result.addContext("user", "testUser");
        auditor.process(result);
        
        File logFile = new File(testAuditDir + "/access.log");
        List<String> lines = Files.readAllLines(logFile.toPath());
        assertTrue("Log should indicate EXECUTE action", lines.get(0).contains("EXECUTE"));
    }

    @Test
    public void testProcessDoesNotThrowExceptions() {
        result = new PipelineResult(testPrompt, "test input");
        result.addContext("user", "testUser");
        
        try {
            auditor.process(result);
            assertTrue("Processing should not throw exception", true);
        } catch (Exception e) {
            fail("Auditor should not throw exception: " + e.getMessage());
        }
    }

    @Test
    public void testAuditWithSpecialCharactersInUser() throws Exception {
        result = new PipelineResult(testPrompt, "test input");
        result.addContext("user", "user@example.com");
        auditor.process(result);
        
        File logFile = new File(testAuditDir + "/access.log");
        List<String> lines = Files.readAllLines(logFile.toPath());
        assertTrue("Log should handle special characters", lines.get(0).contains("user@example.com"));
    }

    @Test
    public void testContextWithoutUserOrSession() throws Exception {
        result = new PipelineResult(testPrompt, "test input");
        auditor.process(result);
        
        File logFile = new File(testAuditDir + "/access.log");
        assertTrue("Log should be created even without context", logFile.exists());
        
        List<String> lines = Files.readAllLines(logFile.toPath());
        assertTrue("Log should have default values", lines.get(0).contains("unknown") && lines.get(0).contains("no-session"));
    }

    @Test
    public void testConcurrentAuditing() throws Exception {
        Thread t1 = new Thread(() -> {
            PipelineResult r = new PipelineResult(testPrompt, "input1");
            r.addContext("user", "user1");
            auditor.process(r);
        });
        
        Thread t2 = new Thread(() -> {
            PipelineResult r = new PipelineResult(testPrompt, "input2");
            r.addContext("user", "user2");
            auditor.process(r);
        });
        
        t1.start();
        t2.start();
        t1.join();
        t2.join();
        
        File logFile = new File(testAuditDir + "/access.log");
        assertTrue("Log file should exist after concurrent access", logFile.exists());
        
        List<String> lines = Files.readAllLines(logFile.toPath());
        assertEquals("Should have both entries", 2, lines.size());
    }

    @Test
    public void testSecurityEventOnlyLoggedWhenFailed() throws Exception {
        result = new PipelineResult(testPrompt, "test input");
        result.addContext("user", "testUser");
        auditor.process(result);
        
        PipelineResult failedResult = new PipelineResult(testPrompt, "test input 2");
        failedResult.addContext("user", "testUser");
        failedResult.fail("Security check failed");
        auditor.process(failedResult);
        
        File securityLogFile = new File(testAuditDir + "/security.log");
        assertTrue("Security log should exist", securityLogFile.exists());
        
        List<String> lines = Files.readAllLines(securityLogFile.toPath());
        assertEquals("Should have only one security event", 1, lines.size());
    }
}
