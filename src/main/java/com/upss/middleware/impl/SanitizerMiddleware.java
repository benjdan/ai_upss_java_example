package com.upss.middleware.impl;

import com.upss.core.BasicSanitizer;
import com.upss.middleware.PipelineResult;
import com.upss.middleware.SecurityMiddleware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SanitizerMiddleware implements SecurityMiddleware {
    private static final Logger logger = LoggerFactory.getLogger(SanitizerMiddleware.class);
    private final BasicSanitizer sanitizer = new BasicSanitizer();

    @Override
    public void process(PipelineResult result) {
        String userInput = result.getUserInput();
        logger.debug("Sanitizing user input for prompt: {}", result.getPrompt().getId());

        if (!sanitizer.isClean(userInput)) {
            logger.warn("Dangerous patterns detected in user input");
            result.fail("Potential prompt injection attempt detected");
            return;
        }

        String sanitized = sanitizer.sanitize(userInput);
        result.setSanitizedInput(sanitized);
        logger.debug("Input sanitization passed");
    }

    @Override
    public String getName() {
        return "SanitizerMiddleware";
    }
}
