package com.upss.middleware;

import com.upss.core.Prompt;
import com.upss.middleware.impl.SanitizerMiddleware;
import com.upss.middleware.impl.ValidatorMiddleware;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for SecurityPipeline
 */
public class SecurityPipelineTest {

    private SecurityPipeline pipeline;
    private Prompt testPrompt;

    @Before
    public void setUp() {
        pipeline = new SecurityPipeline();
        testPrompt = new Prompt("testPrompt", "Test content", "1.0.0", "medium");
    }

    @Test
    public void testPipelineCreation() {
        assertNotNull("Pipeline should be created", pipeline);
        assertEquals("Initial middleware count should be 0", 0, pipeline.getMiddlewareCount());
    }

    @Test
    public void testAddMiddleware() {
        pipeline.use(new ValidatorMiddleware());
        assertEquals("Middleware count should be 1", 1, pipeline.getMiddlewareCount());
        
        pipeline.use(new SanitizerMiddleware());
        assertEquals("Middleware count should be 2", 2, pipeline.getMiddlewareCount());
    }

    @Test
    public void testContextManagement() {
        pipeline.withContext("user", "testUser");
        pipeline.withContext("sessionId", "sess-123");
        
        PipelineResult result = pipeline.execute(testPrompt, "test input");
        assertEquals("User context should be set", "testUser", result.getContext("user"));
        assertEquals("SessionId context should be set", "sess-123", result.getContext("sessionId"));
    }

    @Test
    public void testCleanInputPasses() {
        pipeline.use(new ValidatorMiddleware())
                .use(new SanitizerMiddleware());
        
        PipelineResult result = pipeline.execute(testPrompt, "Safe user input");
        assertTrue("Clean input should pass pipeline", result.isPassed());
    }

    @Test
    public void testDangerousInputFails() {
        pipeline.use(new ValidatorMiddleware())
                .use(new SanitizerMiddleware());
        
        PipelineResult result = pipeline.execute(testPrompt, "SELECT * FROM users");
        assertFalse("Dangerous input should fail pipeline", result.isPassed());
        assertNotNull("Error message should be set", result.getLastError());
    }

    @Test
    public void testOversizedInputFails() {
        int smallLimit = 100;
        pipeline.use(new ValidatorMiddleware(smallLimit));
        
        String largeInput = "x".repeat(smallLimit + 1);
        PipelineResult result = pipeline.execute(testPrompt, largeInput);
        
        assertFalse("Oversized input should fail", result.isPassed());
    }

    @Test
    public void testPipelineChaining() {
        SecurityPipeline chainedPipeline = new SecurityPipeline()
            .withContext("user", "admin")
            .use(new ValidatorMiddleware())
            .use(new SanitizerMiddleware());
        
        assertEquals("Should support method chaining", 2, chainedPipeline.getMiddlewareCount());
    }
}
