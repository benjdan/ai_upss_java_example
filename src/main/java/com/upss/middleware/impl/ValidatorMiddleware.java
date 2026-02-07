package com.upss.middleware.impl;

import com.upss.middleware.PipelineResult;
import com.upss.middleware.SecurityMiddleware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ValidatorMiddleware implements SecurityMiddleware {
    private static final Logger logger = LoggerFactory.getLogger(ValidatorMiddleware.class);
    private static final int DEFAULT_MAX_LENGTH = 32768;
    private final int maxLength;

    public ValidatorMiddleware() {
        this(DEFAULT_MAX_LENGTH);
    }

    public ValidatorMiddleware(int maxLength) {
        this.maxLength = maxLength;
    }

    @Override
    public void process(PipelineResult result) {
        String userInput = result.getUserInput();
        String promptId = result.getPrompt().getId();

        logger.debug("Validating input for prompt: {}", promptId);

        if (userInput.length() > maxLength) {
            logger.warn("User input exceeds maximum length: {} > {}", userInput.length(), maxLength);
            result.fail("Input exceeds maximum allowed length: " + userInput.length() + " > " + maxLength);
            return;
        }

        if (!isValidEncoding(userInput)) {
            logger.warn("Invalid character encoding detected in input");
            result.fail("Invalid character encoding detected");
            return;
        }

        if (result.getPrompt().isCritical()) {
            if (userInput.contains("..") || userInput.contains("//")) {
                logger.warn("Potentially dangerous path traversal patterns detected");
                result.fail("Dangerous patterns detected in critical prompt context");
                return;
            }
        }

        logger.debug("Input validation passed");
    }

    private boolean isValidEncoding(String input) {
        try {
            byte[] bytes = input.getBytes("UTF-8");
            String decoded = new String(bytes, "UTF-8");
            return decoded.equals(input);
        } catch (Exception e) {
            logger.error("Encoding validation failed", e);
            return false;
        }
    }

    @Override
    public String getName() {
        return "ValidatorMiddleware";
    }
}
