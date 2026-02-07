package com.upss.middleware;

import com.upss.core.BasicSanitizer;
import com.upss.core.LightweightAuditor;
import com.upss.core.Prompt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SecurityPipeline {
    private static final Logger logger = LoggerFactory.getLogger(SecurityPipeline.class);
    private final List<SecurityMiddleware> middlewares = new ArrayList<>();
    private final Map<String, Object> context = new HashMap<>();

    public SecurityPipeline withContext(String key, Object value) {
        context.put(key, value);
        return this;
    }

    public SecurityPipeline use(SecurityMiddleware middleware) {
        middlewares.add(middleware);
        logger.info("Added middleware: {}", middleware.getClass().getSimpleName());
        return this;
    }

    public PipelineResult execute(Prompt prompt, String userInput) {
        logger.debug("Executing security pipeline for prompt: {}", prompt.getId());
        
        PipelineResult result = new PipelineResult(prompt, userInput);
        result.addContext(context);

        for (SecurityMiddleware middleware : middlewares) {
            logger.debug("Executing middleware: {}", middleware.getClass().getSimpleName());
            try {
                middleware.process(result);
                
                if (!result.isPassed()) {
                    logger.warn("Security check failed at middleware: {}", middleware.getClass().getSimpleName());
                    result.addError(middleware.getClass().getSimpleName(), result.getLastError());
                    break;
                }
            } catch (Exception e) {
                logger.error("Error executing middleware: {}", middleware.getClass().getSimpleName(), e);
                result.fail("Middleware execution error: " + e.getMessage());
                result.addError(middleware.getClass().getSimpleName(), e.getMessage());
                break;
            }
        }

        logger.info("Pipeline execution completed. Result: {}", result.isPassed() ? "PASSED" : "FAILED");
        return result;
    }

    public int getMiddlewareCount() {
        return middlewares.size();
    }

    @Override
    public String toString() {
        return String.format("SecurityPipeline{middlewares=%d}", middlewares.size());
    }
}
