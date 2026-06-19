package com.upss.core;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.Assert.*;

public class LightweightAuditorTest {

    private String testDir;
    private String testLogPath;
    private LightweightAuditor auditor;

    @Before
    public void setUp() throws IOException {
        testDir = System.getProperty("java.io.tmpdir") + File.separator + "upss_audit_test_" + System.currentTimeMillis();
        new File(testDir).mkdirs();
        testLogPath = testDir + File.separator + "test.log";
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
    public void testAuditorCreation() {
        auditor = new LightweightAuditor(testLogPath);
        assertNotNull("Auditor should be created", auditor);
    }

    @Test
    public void testDefaultAuditorInitialization() {
        auditor = new LightweightAuditor();
        assertNotNull("Default auditor should be created", auditor);
    }

    @Test
    public void testAuditLogFileCreation() throws IOException {
        auditor = new LightweightAuditor(testLogPath);
        
        File logFile = new File(testLogPath);
        assertTrue("Log file should be created", logFile.exists());
    }

    @Test
    public void testLogAccessEntry() throws IOException {
        auditor = new LightweightAuditor(testLogPath);
        
        auditor.logAccess("prompt-1", "EXECUTE", "user-1", "PASSED");
        
        File logFile = new File(testLogPath);
        assertTrue("Log file should contain entries", logFile.length() > 0);
    }

    @Test
    public void testLogAccessEntryFormat() throws IOException {
        auditor = new LightweightAuditor(testLogPath);
        
        auditor.logAccess("prompt-123", "EXECUTE", "admin", "PASSED");
        
        List<String> lines = Files.readAllLines(Paths.get(testLogPath));
        assertTrue("Log file should have entries", lines.size() > 0);
        
        String logEntry = lines.get(0);
        assertTrue("Log should contain timestamp", logEntry.matches("\\[.*\\].*"));
        assertTrue("Log should contain ACTION", logEntry.contains("ACTION=EXECUTE"));
        assertTrue("Log should contain PROMPT", logEntry.contains("PROMPT=prompt-123"));
        assertTrue("Log should contain USER", logEntry.contains("USER=admin"));
        assertTrue("Log should contain STATUS", logEntry.contains("STATUS=PASSED"));
    }

    @Test
    public void testLogModificationEntry() throws IOException {
        auditor = new LightweightAuditor(testLogPath);
        
        auditor.logModification("prompt-2", "2.0.0", "admin-user", "Security patch");
        
        List<String> lines = Files.readAllLines(Paths.get(testLogPath));
        assertTrue("Log should have modification entry", lines.size() > 0);
        
        String logEntry = lines.get(0);
        assertTrue("Log should contain MODIFICATION", logEntry.contains("MODIFICATION"));
        assertTrue("Log should contain PROMPT", logEntry.contains("PROMPT=prompt-2"));
        assertTrue("Log should contain VERSION", logEntry.contains("VERSION=2.0.0"));
        assertTrue("Log should contain MODIFIED_BY", logEntry.contains("MODIFIED_BY=admin-user"));
        assertTrue("Log should contain REASON", logEntry.contains("REASON=Security patch"));
    }

    @Test
    public void testLogSecurityEvent() throws IOException {
        auditor = new LightweightAuditor(testLogPath);
        
        auditor.logSecurityEvent("INJECTION_ATTEMPT", "prompt-3", "Detected SQL injection pattern");
        
        List<String> lines = Files.readAllLines(Paths.get(testLogPath));
        assertTrue("Log should have security event", lines.size() > 0);
        
        String logEntry = lines.get(0);
        assertTrue("Log should contain SECURITY_EVENT", logEntry.contains("SECURITY_EVENT=INJECTION_ATTEMPT"));
        assertTrue("Log should contain PROMPT", logEntry.contains("PROMPT=prompt-3"));
        assertTrue("Log should contain DETAILS", logEntry.contains("DETAILS=Detected SQL injection pattern"));
    }

    @Test
    public void testMultipleLogEntries() throws IOException {
        auditor = new LightweightAuditor(testLogPath);
        
        auditor.logAccess("prompt-1", "EXECUTE", "user-1", "PASSED");
        auditor.logAccess("prompt-2", "EXECUTE", "user-2", "FAILED");
        auditor.logAccess("prompt-3", "EXECUTE", "user-3", "PASSED");
        
        List<String> lines = Files.readAllLines(Paths.get(testLogPath));
        assertEquals("Should have 3 log entries", 3, lines.size());
    }

    @Test
    public void testLogAccessWithDifferentStatuses() throws IOException {
        auditor = new LightweightAuditor(testLogPath);
        
        auditor.logAccess("p1", "EXECUTE", "u1", "PASSED");
        auditor.logAccess("p2", "EXECUTE", "u2", "FAILED");
        auditor.logAccess("p3", "EXECUTE", "u3", "BLOCKED");
        
        List<String> lines = Files.readAllLines(Paths.get(testLogPath));
        assertEquals("Should have 3 entries", 3, lines.size());
        
        assertTrue("First entry should have PASSED", lines.get(0).contains("STATUS=PASSED"));
        assertTrue("Second entry should have FAILED", lines.get(1).contains("STATUS=FAILED"));
        assertTrue("Third entry should have BLOCKED", lines.get(2).contains("STATUS=BLOCKED"));
    }

    @Test
    public void testAuditLogPathGetter() {
        auditor = new LightweightAuditor(testLogPath);
        assertEquals("Audit log path should match", testLogPath, auditor.getAuditLogPath());
    }

    @Test
    public void testAuditLogWithoutParentDirectory() throws IOException {
        String nestedPath = testDir + File.separator + "nested" + File.separator + "deep" + File.separator + "log.txt";
        auditor = new LightweightAuditor(nestedPath);
        
        File logFile = new File(nestedPath);
        assertTrue("Parent directories should be created", logFile.exists() || new File(nestedPath).getParentFile().exists());
    }

    @Test
    public void testLogAccessWithSpecialCharactersInPromptId() throws IOException {
        auditor = new LightweightAuditor(testLogPath);
        
        auditor.logAccess("prompt-with-特殊文字-🔒", "EXECUTE", "user", "PASSED");
        
        List<String> lines = Files.readAllLines(Paths.get(testLogPath));
        assertTrue("Special characters should be logged", lines.get(0).contains("PROMPT="));
    }

    @Test
    public void testLogAccessWithSpecialCharactersInAction() throws IOException {
        auditor = new LightweightAuditor(testLogPath);
        
        auditor.logAccess("prompt", "EXECUTE", "user@domain.com", "PASSED");
        
        List<String> lines = Files.readAllLines(Paths.get(testLogPath));
        assertTrue("User email should be logged", lines.get(0).contains("USER=user@domain.com"));
    }

    @Test
    public void testLogModificationWithLongReason() throws IOException {
        auditor = new LightweightAuditor(testLogPath);
        
        String longReason = "This is a very long reason for modification that spans multiple words and contains detailed information about the changes made.";
        auditor.logModification("prompt", "1.0", "admin", longReason);
        
        List<String> lines = Files.readAllLines(Paths.get(testLogPath));
        assertTrue("Long reason should be logged", lines.get(0).contains(longReason));
    }

    @Test
    public void testLogSecurityEventWithComplexDetails() throws IOException {
        auditor = new LightweightAuditor(testLogPath);
        
        String complexDetails = "Detected pattern: 'DROP DATABASE' in context of 'user input' from IP 192.168.1.1";
        auditor.logSecurityEvent("SQL_INJECTION", "prompt", complexDetails);
        
        List<String> lines = Files.readAllLines(Paths.get(testLogPath));
        assertTrue("Complex details should be logged", lines.get(0).contains(complexDetails));
    }

    @Test
    public void testLogEntryTimestampIncrement() throws IOException, InterruptedException {
        auditor = new LightweightAuditor(testLogPath);
        
        auditor.logAccess("p1", "EXECUTE", "u1", "PASSED");
        Thread.sleep(10);
        auditor.logAccess("p2", "EXECUTE", "u2", "PASSED");
        
        List<String> lines = Files.readAllLines(Paths.get(testLogPath));
        assertEquals("Should have 2 entries", 2, lines.size());
        
        assertNotEquals("Entries should be different", lines.get(0), lines.get(1));
    }

    @Test
    public void testConcurrentLogging() throws IOException, InterruptedException {
        auditor = new LightweightAuditor(testLogPath);
        
        Thread t1 = new Thread(() -> {
            for (int i = 0; i < 5; i++) {
                auditor.logAccess("p" + i, "EXECUTE", "u1", "PASSED");
            }
        });
        
        Thread t2 = new Thread(() -> {
            for (int i = 5; i < 10; i++) {
                auditor.logAccess("p" + i, "EXECUTE", "u2", "PASSED");
            }
        });
        
        t1.start();
        t2.start();
        t1.join();
        t2.join();
        
        List<String> lines = Files.readAllLines(Paths.get(testLogPath));
        
        assertTrue("Should have logged entries from both threads", lines.size() >= 10);
    }

    @Test
    public void testDifferentAuditorsIndependence() throws IOException {
        String logPath1 = testDir + File.separator + "log1.txt";
        String logPath2 = testDir + File.separator + "log2.txt";
        
        LightweightAuditor auditor1 = new LightweightAuditor(logPath1);
        LightweightAuditor auditor2 = new LightweightAuditor(logPath2);
        
        auditor1.logAccess("p1", "EXECUTE", "u1", "PASSED");
        auditor2.logAccess("p2", "EXECUTE", "u2", "FAILED");
        
        List<String> lines1 = Files.readAllLines(Paths.get(logPath1));
        List<String> lines2 = Files.readAllLines(Paths.get(logPath2));
        
        assertEquals("File 1 should have 1 entry", 1, lines1.size());
        assertEquals("File 2 should have 1 entry", 1, lines2.size());
        assertTrue("Files should have different content", !lines1.get(0).equals(lines2.get(0)));
    }

    @Test
    public void testLogAppend() throws IOException {
        auditor = new LightweightAuditor(testLogPath);
        
        auditor.logAccess("p1", "EXECUTE", "u1", "PASSED");
        
        List<String> lines1 = Files.readAllLines(Paths.get(testLogPath));
        assertEquals("First write should have 1 entry", 1, lines1.size());
        
        auditor.logAccess("p2", "EXECUTE", "u2", "PASSED");
        
        List<String> lines2 = Files.readAllLines(Paths.get(testLogPath));
        assertEquals("Second write should append, resulting in 2 entries", 2, lines2.size());
    }
}
