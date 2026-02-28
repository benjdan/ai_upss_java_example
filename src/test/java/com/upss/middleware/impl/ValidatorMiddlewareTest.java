package com.upss.middleware.impl;

import com.upss.core.Prompt;
import com.upss.middleware.PipelineResult;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class ValidatorMiddlewareTest {

    private ValidatorMiddleware validator;
    private PipelineResult result;
    private Prompt testPrompt;

    @Before
    public void setUp() {
        validator = new ValidatorMiddleware();
        testPrompt = new Prompt("test-prompt", "Test content", "1.0.0", "medium");
        result = new PipelineResult(testPrompt, "test input");
    }

    @Test
    public void testValidatorCreationWithDefault() {
        assertNotNull("Validator should be created", validator);
        assertEquals("Should use default max length", 32768, validator.maxLength);
    }

    @Test
    public void testValidatorCreationWithCustomLength() {
        ValidatorMiddleware customValidator = new ValidatorMiddleware(1000);
        assertEquals("Should use custom max length", 1000, customValidator.maxLength);
    }

    @Test
    public void testValidInputPasses() {
        result = new PipelineResult(testPrompt, "This is a safe input");
        validator.process(result);
        assertTrue("Valid input should pass validation", result.isPassed());
    }

    @Test
    public void testInputLengthAtLimit() {
        ValidatorMiddleware shortValidator = new ValidatorMiddleware(100);
        String input = "x".repeat(100);
        result = new PipelineResult(testPrompt, input);
        shortValidator.process(result);
        assertTrue("Input at max length should pass", result.isPassed());
    }

    @Test
    public void testInputLengthExceedsLimit() {
        ValidatorMiddleware shortValidator = new ValidatorMiddleware(100);
        String input = "x".repeat(101);
        result = new PipelineResult(testPrompt, input);
        shortValidator.process(result);
        
        assertFalse("Input exceeding max length should fail", result.isPassed());
        assertTrue("Error message should mention length exceeded", result.getLastError().contains("exceeds maximum"));
    }

    @Test
    public void testEmptyInputValidation() {
        result = new PipelineResult(testPrompt, "");
        validator.process(result);
        assertTrue("Empty input should pass validation", result.isPassed());
    }

    @Test
    public void testValidUTF8Encoding() {
        result = new PipelineResult(testPrompt, "Hello 世界 مرحبا");
        validator.process(result);
        assertTrue("Valid UTF-8 should pass", result.isPassed());
    }

    @Test
    public void testSpecialCharacterValidation() {
        result = new PipelineResult(testPrompt, "Special: !@#$%^&*()_+-=[]{}|;:',.<>?");
        validator.process(result);
        assertTrue("Special characters should be valid", result.isPassed());
    }

    @Test
    public void testPathTraversalInCriticalPrompt() {
        Prompt criticalPrompt = new Prompt("critical", "Critical content", "1.0.0", "critical");
        result = new PipelineResult(criticalPrompt, "input with .. inside");
        validator.process(result);
        
        assertFalse("Path traversal in critical prompt should fail", result.isPassed());
        assertTrue("Error should mention dangerous patterns", result.getLastError().contains("Dangerous"));
    }

    @Test
    public void testDoubleSlashInCriticalPrompt() {
        Prompt criticalPrompt = new Prompt("critical", "Critical content", "1.0.0", "critical");
        result = new PipelineResult(criticalPrompt, "input with // inside");
        validator.process(result);
        
        assertFalse("Double slash in critical prompt should fail", result.isPassed());
    }

    @Test
    public void testPathTraversalInNonCriticalPrompt() {
        Prompt normalPrompt = new Prompt("normal", "Normal content", "1.0.0", "low");
        result = new PipelineResult(normalPrompt, "input with .. inside");
        validator.process(result);
        
        assertTrue("Path traversal in non-critical prompt should pass", result.isPassed());
    }

    @Test
    public void testMultiplePathTraversalPatterns() {
        Prompt criticalPrompt = new Prompt("critical", "Critical content", "1.0.0", "critical");
        result = new PipelineResult(criticalPrompt, "../../etc/passwd");
        validator.process(result);
        
        assertFalse("Multiple traversal patterns should fail", result.isPassed());
    }

    @Test
    public void testWhitespaceValidation() {
        result = new PipelineResult(testPrompt, "Text with   spaces\t\nand\nnewlines");
        validator.process(result);
        assertTrue("Whitespace should be valid", result.isPassed());
    }

    @Test
    public void testNumbersAndSymbols() {
        result = new PipelineResult(testPrompt, "Numbers 1234567890 and symbols @#$%");
        validator.process(result);
        assertTrue("Numbers and symbols should be valid", result.isPassed());
    }

    @Test
    public void testMaxLengthBoundary() {
        ValidatorMiddleware limitedValidator = new ValidatorMiddleware(50);
        
        String input49 = "x".repeat(49);
        result = new PipelineResult(testPrompt, input49);
        limitedValidator.process(result);
        assertTrue("Input just under limit should pass", result.isPassed());
    }

    @Test
    public void testGetName() {
        assertEquals("Name should be ValidatorMiddleware", "ValidatorMiddleware", validator.getName());
    }

    @Test
    public void testMultipleBadPatternsInCritical() {
        Prompt criticalPrompt = new Prompt("critical", "Critical content", "1.0.0", "critical");
        result = new PipelineResult(criticalPrompt, "test .. and //");
        validator.process(result);
        
        assertFalse("Multiple bad patterns should fail", result.isPassed());
    }

    @Test
    public void testProcessResultStateUnchangedOnSuccess() {
        result = new PipelineResult(testPrompt, "Valid input");
        assertTrue("Result should pass initially", result.isPassed());
        validator.process(result);
        assertTrue("Result should still pass after validation", result.isPassed());
    }

    @Test
    public void testProcessResultStateChangeOnFailure() {
        ValidatorMiddleware limitedValidator = new ValidatorMiddleware(10);
        result = new PipelineResult(testPrompt, "This is too long");
        assertTrue("Result should pass initially", result.isPassed());
        limitedValidator.process(result);
        assertFalse("Result should fail after validation", result.isPassed());
    }
}
