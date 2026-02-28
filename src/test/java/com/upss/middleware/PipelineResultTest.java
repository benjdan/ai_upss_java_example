package com.upss.middleware;

import com.upss.core.Prompt;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class PipelineResultTest {

    private PipelineResult result;
    private Prompt testPrompt;

    @Before
    public void setUp() {
        testPrompt = new Prompt("test-prompt", "Test content", "1.0.0", "medium");
        result = new PipelineResult(testPrompt, "test input");
    }

    @Test
    public void testPipelineResultCreation() {
        assertNotNull("Result should be created", result);
        assertEquals("Prompt should match", testPrompt, result.getPrompt());
        assertEquals("User input should match", "test input", result.getUserInput());
    }

    @Test
    public void testDefaultPassedStatus() {
        assertTrue("Result should pass by default", result.isPassed());
    }

    @Test
    public void testSanitizedInputInitialization() {
        String userInput = "test input";
        PipelineResult r = new PipelineResult(testPrompt, userInput);
        assertEquals("Sanitized input should equal user input initially", userInput, r.getSanitizedInput());
    }

    @Test
    public void testFailResult() {
        result.fail("Test error");
        
        assertFalse("Result should fail", result.isPassed());
        assertEquals("Error message should match", "Test error", result.getLastError());
    }

    @Test
    public void testSetSanitizedInput() {
        String sanitized = "sanitized input";
        result.setSanitizedInput(sanitized);
        
        assertEquals("Sanitized input should be updated", sanitized, result.getSanitizedInput());
    }

    @Test
    public void testAddContextWithKeyValue() {
        result.addContext("user", "testUser");
        result.addContext("sessionId", "sess-123");
        
        assertEquals("User context should be set", "testUser", result.getContext("user"));
        assertEquals("SessionId context should be set", "sess-123", result.getContext("sessionId"));
    }

    @Test
    public void testAddContextWithMap() {
        Map<String, Object> contextData = new HashMap<>();
        contextData.put("user", "admin");
        contextData.put("ip", "192.168.1.1");
        contextData.put("timestamp", System.currentTimeMillis());
        
        result.addContext(contextData);
        
        assertEquals("User should be in context", "admin", result.getContext("user"));
        assertEquals("IP should be in context", "192.168.1.1", result.getContext("ip"));
        assertNotNull("Timestamp should be in context", result.getContext("timestamp"));
    }

    @Test
    public void testGetAllContext() {
        result.addContext("key1", "value1");
        result.addContext("key2", "value2");
        
        Map<String, Object> allContext = result.getAllContext();
        
        assertEquals("Should have 2 context entries", 2, allContext.size());
        assertEquals("Key1 should be present", "value1", allContext.get("key1"));
        assertEquals("Key2 should be present", "value2", allContext.get("key2"));
    }

    @Test
    public void testGetContextNotFound() {
        assertNull("Non-existent context should return null", result.getContext("nonexistent"));
    }

    @Test
    public void testAddError() {
        result.addError("ValidatorMiddleware", "Input too long");
        result.addError("SanitizerMiddleware", "Injection pattern detected");
        
        Map<String, String> errors = result.getErrors();
        
        assertEquals("Should have 2 errors", 2, errors.size());
        assertEquals("Validator error should match", "Input too long", errors.get("ValidatorMiddleware"));
        assertEquals("Sanitizer error should match", "Injection pattern detected", errors.get("SanitizerMiddleware"));
    }

    @Test
    public void testGetErrors() {
        result.addError("stage1", "error1");
        result.addError("stage2", "error2");
        
        Map<String, String> errors = result.getErrors();
        
        assertNotNull("Errors should not be null", errors);
        assertTrue("Should contain stage1", errors.containsKey("stage1"));
        assertTrue("Should contain stage2", errors.containsKey("stage2"));
    }

    @Test
    public void testPromptGetter() {
        Prompt prompt = result.getPrompt();
        
        assertNotNull("Prompt should not be null", prompt);
        assertEquals("Prompt ID should match", "test-prompt", prompt.getId());
    }

    @Test
    public void testUserInputGetter() {
        String input = "test input";
        PipelineResult r = new PipelineResult(testPrompt, input);
        
        assertEquals("User input should match", input, r.getUserInput());
    }

    @Test
    public void testMultipleFailCalls() {
        result.fail("First error");
        String firstError = result.getLastError();
        
        result.fail("Second error");
        String secondError = result.getLastError();
        
        assertEquals("First error should be recorded", "First error", firstError);
        assertEquals("Second error should override", "Second error", secondError);
        assertFalse("Result should still fail", result.isPassed());
    }

    @Test
    public void testContextNotModifiable() {
        result.addContext("original", "value");
        Map<String, Object> context = result.getAllContext();
        context.put("modified", "value");
        
        assertNull("Original context should not be modified", result.getContext("modified"));
    }

    @Test
    public void testErrorsNotModifiable() {
        result.addError("stage1", "error");
        Map<String, String> errors = result.getErrors();
        errors.put("stage2", "error");
        
        assertNull("Original errors should not be modified", result.getErrors().get("stage2"));
    }

    @Test
    public void testPipelineResultWithDifferentPrompts() {
        Prompt prompt1 = new Prompt("p1", "content1", "1.0", "low");
        Prompt prompt2 = new Prompt("p2", "content2", "2.0", "high");
        
        PipelineResult result1 = new PipelineResult(prompt1, "input1");
        PipelineResult result2 = new PipelineResult(prompt2, "input2");
        
        assertEquals("Result1 should have prompt1", prompt1, result1.getPrompt());
        assertEquals("Result2 should have prompt2", prompt2, result2.getPrompt());
        assertNotEquals("Results should have different prompts", result1.getPrompt(), result2.getPrompt());
    }

    @Test
    public void testContextWithNullValue() {
        result.addContext("nullKey", null);
        
        assertTrue("Context should contain null key", result.getAllContext().containsKey("nullKey"));
        assertNull("Value should be null", result.getContext("nullKey"));
    }

    @Test
    public void testSanitizedInputDifferentFromUserInput() {
        PipelineResult r = new PipelineResult(testPrompt, "original");
        r.setSanitizedInput("modified");
        
        assertEquals("User input should remain original", "original", r.getUserInput());
        assertEquals("Sanitized input should be modified", "modified", r.getSanitizedInput());
    }

    @Test
    public void testContextTypePreservation() {
        Integer intValue = 42;
        Double doubleValue = 3.14;
        Boolean boolValue = true;
        
        result.addContext("int", intValue);
        result.addContext("double", doubleValue);
        result.addContext("bool", boolValue);
        
        assertEquals("Integer should be preserved", intValue, result.getContext("int"));
        assertEquals("Double should be preserved", doubleValue, result.getContext("double"));
        assertEquals("Boolean should be preserved", boolValue, result.getContext("bool"));
    }

    @Test
    public void testFailWithEmptyString() {
        result.fail("");
        
        assertFalse("Result should fail", result.isPassed());
        assertEquals("Error should be empty string", "", result.getLastError());
    }

    @Test
    public void testFailWithNullMessage() {
        result.fail(null);
        
        assertFalse("Result should fail", result.isPassed());
        assertNull("Error message should be null", result.getLastError());
    }

    @Test
    public void testLargeContext() {
        Map<String, Object> largeContext = new HashMap<>();
        for (int i = 0; i < 1000; i++) {
            largeContext.put("key" + i, "value" + i);
        }
        
        result.addContext(largeContext);
        
        Map<String, Object> retrieved = result.getAllContext();
        assertEquals("Should have all context entries", 1000, retrieved.size());
        assertEquals("Should be able to retrieve specific entry", "value500", retrieved.get("key500"));
    }
}
