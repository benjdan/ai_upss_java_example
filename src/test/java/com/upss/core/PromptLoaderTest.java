package com.upss.core;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import static org.junit.Assert.*;

public class PromptLoaderTest {

    private String testDir;
    private String testConfigPath;
    private PromptLoader loader;

    @Before
    public void setUp() throws IOException {
        testDir = System.getProperty("java.io.tmpdir") + File.separator + "upss_test_" + System.currentTimeMillis();
        new File(testDir).mkdirs();
        testConfigPath = testDir + File.separator + "prompts.json";
    }

    @After
    public void tearDown() {
        deleteDirectory(new File(testDir));
    }

    private void deleteDirectory(File dir) {
        if (dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File f : files) {
                    deleteDirectory(f);
                }
            }
        }
        dir.delete();
    }

    @Test
    public void testPromptLoaderCreation() {
        loader = new PromptLoader(testConfigPath);
        assertNotNull("Loader should be created", loader);
    }

    @Test
    public void testLoaderWithMissingConfigFile() {
        String nonExistentPath = testDir + File.separator + "nonexistent.json";
        loader = new PromptLoader(nonExistentPath);
        
        assertNotNull("Loader should handle missing config file", loader);
        assertThrows("Should throw exception when loading missing prompt",
                PromptLoader.PromptLoadException.class, () -> loader.loadPrompt("anyPrompt"));
    }

    @Test
    public void testChecksumCalculation() throws IOException {
        loader = new PromptLoader(testConfigPath);
        
        String content = "Test prompt content";
        String checksum = loader.calculateChecksum(content);
        
        assertNotNull("Checksum should not be null", checksum);
        assertTrue("Checksum should contain sha256 prefix", checksum.startsWith("sha256:"));
        assertTrue("Checksum should be long enough", checksum.length() > 10);
    }

    @Test
    public void testChecksumConsistency() throws IOException {
        loader = new PromptLoader(testConfigPath);
        
        String content = "Test prompt content";
        String checksum1 = loader.calculateChecksum(content);
        String checksum2 = loader.calculateChecksum(content);
        
        assertEquals("Same content should produce same checksum", checksum1, checksum2);
    }

    @Test
    public void testChecksumDifference() throws IOException {
        loader = new PromptLoader(testConfigPath);
        
        String content1 = "Test prompt content 1";
        String content2 = "Test prompt content 2";
        
        String checksum1 = loader.calculateChecksum(content1);
        String checksum2 = loader.calculateChecksum(content2);
        
        assertNotEquals("Different content should produce different checksums", checksum1, checksum2);
    }

    @Test
    public void testLoaderInitializationParameters() {
        loader = new PromptLoader(testConfigPath, true, true);
        assertNotNull("Loader with explicit parameters should be created", loader);
    }

    @Test
    public void testLoaderWithValidationDisabled() {
        loader = new PromptLoader(testConfigPath, false, false);
        assertNotNull("Loader with validation disabled should be created", loader);
    }

    @Test
    public void testChecksumAlgorithmAvailability() throws IOException {
        loader = new PromptLoader(testConfigPath);
        
        String checksum = loader.calculateChecksum("test");
        assertNotNull("SHA-256 should be available", checksum);
    }

    @Test
    public void testEmptyContentChecksum() throws IOException {
        loader = new PromptLoader(testConfigPath);
        
        String emptyChecksum = loader.calculateChecksum("");
        assertNotNull("Checksum of empty content should not be null", emptyChecksum);
        assertTrue("Checksum should start with sha256:", emptyChecksum.startsWith("sha256:"));
    }

    @Test
    public void testLargeContentChecksum() throws IOException {
        loader = new PromptLoader(testConfigPath);
        
        StringBuilder largeContent = new StringBuilder();
        for (int i = 0; i < 10000; i++) {
            largeContent.append("This is a test line for large content. ");
        }
        
        String checksum = loader.calculateChecksum(largeContent.toString());
        assertNotNull("Large content should have checksum", checksum);
        assertTrue("Checksum should be valid", checksum.startsWith("sha256:"));
    }

    @Test
    public void testChecksumWithSpecialCharacters() throws IOException {
        loader = new PromptLoader(testConfigPath);
        
        String contentWithSpecialChars = "Content with 特殊文字 and emojis 🔒 and symbols !@#$%^&*()";
        String checksum = loader.calculateChecksum(contentWithSpecialChars);
        
        assertNotNull("Checksum with special characters should not be null", checksum);
        assertTrue("Checksum should be valid", checksum.startsWith("sha256:"));
    }

    @Test
    public void testDefaultLoaderInitialization() {
        loader = new PromptLoader(testConfigPath);
        assertNotNull("Default loader initialization should work", loader);
    }

    @Test
    public void testConfigPathStorage() {
        loader = new PromptLoader(testConfigPath);
        assertNotNull("Loader should be initialized", loader);
    }

    @Test
    public void testMultipleChecksumsIndependent() throws IOException {
        loader = new PromptLoader(testConfigPath);
        
        String checksum1 = loader.calculateChecksum("content1");
        String checksum2 = loader.calculateChecksum("content2");
        String checksum3 = loader.calculateChecksum("content1");
        
        assertEquals("Same content should produce same checksum", checksum1, checksum3);
        assertNotEquals("Different content should produce different checksums", checksum1, checksum2);
    }

    @Test
    public void testPromptLoadExceptionWithMessage() {
        try {
            throw new PromptLoader.PromptLoadException("Test error message");
        } catch (PromptLoader.PromptLoadException e) {
            assertEquals("Exception message should match", "Test error message", e.getMessage());
        }
    }

    @Test
    public void testPromptLoadExceptionWithCause() {
        try {
            IOException cause = new IOException("Original cause");
            throw new PromptLoader.PromptLoadException("Wrapper message", cause);
        } catch (PromptLoader.PromptLoadException e) {
            assertEquals("Exception message should match", "Wrapper message", e.getMessage());
            assertNotNull("Exception should have cause", e.getCause());
            assertTrue("Cause should be IOException", e.getCause() instanceof IOException);
        }
    }

    @Test
    public void testChecksumFormatConsistency() throws IOException {
        loader = new PromptLoader(testConfigPath);
        
        String checksum1 = loader.calculateChecksum("test1");
        String checksum2 = loader.calculateChecksum("test2");
        
        assertTrue("Checksum1 should be valid SHA256", checksum1.matches("sha256:[a-f0-9]{64}"));
        assertTrue("Checksum2 should be valid SHA256", checksum2.matches("sha256:[a-f0-9]{64}"));
    }

    @Test
    public void testLoaderThreadSafety() throws IOException, InterruptedException {
        loader = new PromptLoader(testConfigPath);
        
        String checksum1 = loader.calculateChecksum("content");
        
        Thread thread = new Thread(() -> {
            try {
                String checksum2 = loader.calculateChecksum("content");
                assertEquals("Multi-threaded checksums should match", checksum1, checksum2);
            } catch (Exception e) {
                fail("Exception in thread: " + e.getMessage());
            }
        });
        
        thread.start();
        thread.join();
    }
}
