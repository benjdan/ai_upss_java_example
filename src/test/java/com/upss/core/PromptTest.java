package com.upss.core;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;


 public class PromptTest {

    private Prompt prompt;

    @Before
    public void setUp() {
        prompt = new Prompt("test-prompt", "Test prompt content", "1.0.0", "medium");
    }

    @Test
    public void testPromptCreation() {
        assertNotNull("Prompt should be created", prompt);
        assertEquals("ID should match", "test-prompt", prompt.getId());
        assertEquals("Content should match", "Test prompt content", prompt.getContent());
        assertEquals("Version should match", "1.0.0", prompt.getVersion());
        assertEquals("Risk level should match", "medium", prompt.getRiskLevel());
    }

    @Test
    public void testPromptLoadedAtTime() {
        long beforeCreation = System.currentTimeMillis();
        Prompt newPrompt = new Prompt("id", "content", "1.0", "low");
        long afterCreation = System.currentTimeMillis();

        assertTrue("LoadedAt should be set", newPrompt.getLoadedAt() > 0);
        assertTrue("LoadedAt should be recent", newPrompt.getLoadedAt() >= beforeCreation);
        assertTrue("LoadedAt should not be in future", newPrompt.getLoadedAt() <= afterCreation + 100);
    }

    @Test
    public void testIsCriticalTrue() {
        Prompt criticalPrompt = new Prompt("id", "content", "1.0", "critical");
        assertTrue("Critical prompt should return true", criticalPrompt.isCritical());
    }

    @Test
    public void testIsCriticalFalse() {
        Prompt nonCriticalPrompt = new Prompt("id", "content", "1.0", "medium");
        assertFalse("Non-critical prompt should return false", nonCriticalPrompt.isCritical());
    }

    @Test
    public void testIsCriticalCaseInsensitive() {
        Prompt criticalPrompt = new Prompt("id", "content", "1.0", "CRITICAL");
        assertTrue("Critical (uppercase) prompt should return true", criticalPrompt.isCritical());
        
        Prompt mixedCasePrompt = new Prompt("id", "content", "1.0", "CrItIcAl");
        assertTrue("Critical (mixed case) prompt should return true", mixedCasePrompt.isCritical());
    }

    @Test
    public void testGettersWork() {
        String id = "my-prompt";
        String content = "Prompt content";
        String version = "2.1.0";
        String riskLevel = "high";
        
        Prompt p = new Prompt(id, content, version, riskLevel);
        
        assertEquals("getId should return correct value", id, p.getId());
        assertEquals("getContent should return correct value", content, p.getContent());
        assertEquals("getVersion should return correct value", version, p.getVersion());
        assertEquals("getRiskLevel should return correct value", riskLevel, p.getRiskLevel());
    }

    @Test
    public void testToString() {
        String str = prompt.toString();
        
        assertNotNull("toString should not return null", str);
        assertTrue("toString should contain prompt ID", str.contains("test-prompt"));
        assertTrue("toString should contain version", str.contains("1.0.0"));
        assertTrue("toString should contain risk level", str.contains("medium"));
    }

    @Test
    public void testDifferentRiskLevels() {
        Prompt lowRisk = new Prompt("id1", "content", "1.0", "low");
        Prompt mediumRisk = new Prompt("id2", "content", "1.0", "medium");
        Prompt highRisk = new Prompt("id3", "content", "1.0", "high");
        Prompt criticalRisk = new Prompt("id4", "content", "1.0", "critical");
        
        assertEquals("Low risk level should be set", "low", lowRisk.getRiskLevel());
        assertEquals("Medium risk level should be set", "medium", mediumRisk.getRiskLevel());
        assertEquals("High risk level should be set", "high", highRisk.getRiskLevel());
        assertEquals("Critical risk level should be set", "critical", criticalRisk.getRiskLevel());
    }

    @Test
    public void testEmptyContent() {
        Prompt emptyPrompt = new Prompt("id", "", "1.0", "low");
        assertEquals("Empty content should be stored", "", emptyPrompt.getContent());
    }

    @Test
    public void testNullContent() {
        Prompt nullPrompt = new Prompt("id", null, "1.0", "low");
        assertNull("Null content should be stored as null", nullPrompt.getContent());
    }

    @Test
    public void testVersionFormatting() {
        Prompt v1 = new Prompt("id", "content", "1.0.0", "low");
        Prompt v2 = new Prompt("id", "content", "2.0.0-beta", "low");
        Prompt v3 = new Prompt("id", "content", "1.0.0-rc1", "low");
        
        assertEquals("Version 1.0.0 should be stored", "1.0.0", v1.getVersion());
        assertEquals("Version 2.0.0-beta should be stored", "2.0.0-beta", v2.getVersion());
        assertEquals("Version 1.0.0-rc1 should be stored", "1.0.0-rc1", v3.getVersion());
    }

    @Test
    public void testMultipleInstancesIndependent() {
        Prompt prompt1 = new Prompt("id1", "content1", "1.0", "low");
        long time1 = prompt1.getLoadedAt();
        
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        Prompt prompt2 = new Prompt("id2", "content2", "2.0", "high");
        long time2 = prompt2.getLoadedAt();
        
        assertNotEquals("Different prompts should have different timestamps", time1, time2);
        assertTrue("Second prompt should have later timestamp", time2 > time1);
    }

    @Test
    public void testPromptWithSpecialCharacters() {
        String contentWithSpecialChars = "Content with 特殊文字 and emojis 🔒";
        Prompt specialPrompt = new Prompt("special-id", contentWithSpecialChars, "1.0", "low");
        
        assertEquals("Special characters should be preserved", contentWithSpecialChars, specialPrompt.getContent());
    }

    @Test
    public void testPromptWithLongContent() {
        StringBuilder longContent = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            longContent.append("This is a very long prompt content line. ");
        }
        
        Prompt longPrompt = new Prompt("long-id", longContent.toString(), "1.0", "low");
        assertEquals("Long content should be stored", longContent.toString(), longPrompt.getContent());
    }

    @Test
    public void testPromptImmutability() {
        String content = "Original content";
        Prompt p = new Prompt("id", content, "1.0", "low");
                
        String retrieved = p.getContent();
        assertEquals("Content should match original", content, retrieved);
                
        Prompt p2 = new Prompt("id2", "Different content", "1.0", "low");
        assertEquals("First prompt content should not change", content, p.getContent());
    }
}
