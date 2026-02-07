package com.upss.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BasicSanitizer {
    private static final Logger logger = LoggerFactory.getLogger(BasicSanitizer.class);

    private static final String[] INJECTION_PATTERNS = {
            "SELECT", "INSERT", "UPDATE", "DELETE", "DROP",
            "exec", "execute", "script", "javascript",
            "eval", "system", "os.system", "__import__",
            "subprocess", "Popen", "cmd.exe", "/bin/bash"
    };

    public String sanitize(String prompt) {
        if (prompt == null || prompt.isEmpty()) {
            return prompt;
        }

        logger.info("Sanitizing prompt input");
        String sanitized = prompt;

        for (String pattern : INJECTION_PATTERNS) {
            if (sanitized.toLowerCase().contains(pattern.toLowerCase())) {
                logger.warn("Detected potential injection pattern: {}", pattern);
                sanitized = sanitized.replaceAll("(?i)" + pattern, "");
            }
        }

        logger.info("Sanitization complete");
        return sanitized.trim();
    }

    public boolean isClean(String prompt) {
        if (prompt == null) {
            return true;
        }

        for (String pattern : INJECTION_PATTERNS) {
            if (prompt.toLowerCase().contains(pattern.toLowerCase())) {
                return false;
            }
        }
        return true;
    }
}
