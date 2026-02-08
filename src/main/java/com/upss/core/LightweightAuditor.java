package com.upss.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class LightweightAuditor {
    private static final Logger logger = LoggerFactory.getLogger(LightweightAuditor.class);
    private final String auditLogPath;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public LightweightAuditor(String auditLogPath) {
        this.auditLogPath = auditLogPath;
        ensureLogFileExists();
    }

    public LightweightAuditor() {
        this("./config/audit/prompts.log");
    }

    private void ensureLogFileExists() {
        try {
            File logFile = new File(auditLogPath);
            File parentDir = logFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }
            if (!logFile.exists()) {
                logFile.createNewFile();
            }
        } catch (IOException e) {
            logger.error("Failed to create audit log file = {}", e);
        }
    }

    public void logAccess(String promptId, String action, String user, String status) {
        String timestamp = dateFormat.format(new Date());
        String logEntry = String.format(
                "[%s] ACTION=%s | PROMPT=%s | USER=%s | STATUS=%s",
                timestamp, action, promptId, user, status
        );
        writeToLog(logEntry);
        logger.info(logEntry);
    }

    public void logModification(String promptId, String version, String modifiedBy, String reason) {
        String timestamp = dateFormat.format(new Date());
        String logEntry = String.format(
                "[%s] MODIFICATION | PROMPT=%s | VERSION=%s | MODIFIED_BY=%s | REASON=%s",
                timestamp, promptId, version, modifiedBy, reason
        );
        writeToLog(logEntry);
        logger.info(logEntry);
    }

    public void logSecurityEvent(String eventType, String promptId, String details) {
        String timestamp = dateFormat.format(new Date());
        String logEntry = String.format(
                "[%s] SECURITY_EVENT=%s | PROMPT=%s | DETAILS=%s",
                timestamp, eventType, promptId, details
        );
        writeToLog(logEntry);
        logger.warn(logEntry);
    }

    private synchronized void writeToLog(String entry) {
        try (FileWriter fw = new FileWriter(auditLogPath, true)) {
            fw.write(entry + "\n");
        } catch (IOException e) {
            logger.error("Failed to write audit log entry = {}", e);
        }
    }

    public String getAuditLogPath() {
        return auditLogPath;
    }
}
