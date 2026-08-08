package com.upss.core;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString(exclude = "loadedAt")
public class Prompt {
    private final String id;
    private final String content;
    private final String version;
    private final String riskLevel;
    private final long loadedAt;

    public Prompt(String id, String content, String version, String riskLevel) {
        this.id = id;
        this.content = content;
        this.version = version;
        this.riskLevel = riskLevel;
        this.loadedAt = System.currentTimeMillis();
    }

    public boolean isCritical() {
        return "critical".equalsIgnoreCase(riskLevel);
    }
}
