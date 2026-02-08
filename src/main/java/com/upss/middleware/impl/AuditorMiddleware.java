package com.upss.middleware.impl;

import com.upss.core.LightweightAuditor;
import com.upss.middleware.PipelineResult;
import com.upss.middleware.SecurityMiddleware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuditorMiddleware implements SecurityMiddleware {
    private static final Logger logger = LoggerFactory.getLogger(AuditorMiddleware.class);
    private final LightweightAuditor auditor;

    public AuditorMiddleware(LightweightAuditor auditor) {
        this.auditor = auditor;
    }

    public AuditorMiddleware() {
        this(new LightweightAuditor());
    }

    @Override
    public void process(PipelineResult result) {
        String promptId = result.getPrompt().getId();
        String user = (String) result.getContext("user");
        String sessionId = (String) result.getContext("sessionId");

        if (user == null) {
            user = "unknown";
        }
        if (sessionId == null) {
            sessionId = "no-session";
        }

        auditor.logAccess(promptId, "EXECUTE", user, result.isPassed() ? "PASSED" : "FAILED");

        if (!result.isPassed()) {
            auditor.logSecurityEvent("SECURITY_CHECK_FAILED", promptId, result.getLastError());
        }

        logger.debug("Audit log entry created for prompt: {}", promptId);
    }

    @Override
    public String getName() {
        return "AuditorMiddleware";
    }

    public LightweightAuditor getAuditor() {
        return auditor;
    }
}
