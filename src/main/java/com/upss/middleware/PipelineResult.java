package com.upss.middleware;

import com.upss.core.Prompt;

import java.util.HashMap;
import java.util.Map;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Builder
public class PipelineResult {
    private final Prompt prompt;
    private final String userInput;
    private final Map<String, Object> context = new HashMap<>();
    private final Map<String, String> errors = new HashMap<>();
    private boolean passed = true;
    private String lastError = null;
    private String sanitizedInput = null;

    public PipelineResult(Prompt prompt, String userInput) {
        this.prompt = prompt;
        this.userInput = userInput;
        this.sanitizedInput = userInput;
    }

    public boolean isPassed() {
        return passed;
    }

    public void fail(String error) {
        this.passed = false;
        this.lastError = error;
    }

    public void addContext(String key, Object value) {
        context.put(key, value);
    }

    public void addContext(Map<String, Object> contextData) {
        context.putAll(contextData);
    }

    public Map<String, Object> getAllContext() {
        return new HashMap<>(context);
    }

    public void addError(String stage, String errorMessage) {
        errors.put(stage, errorMessage);
    }

    @Override
    public String toString() {
        return String.format(
                "PipelineResult{prompt='%s', passed=%s, errors=%s}",
                prompt.getId(), passed, errors.size()
        );
    }
}
