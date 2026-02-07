package com.upss.core;

// import lombok.AllArgsConstructor;
// import lombok.Builder;
// import lombok.Data;
// import lombok.NoArgsConstructor;

// @Data
// @NoArgsConstructor
// @AllArgsConstructor
// @Builder
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

    public String getId() {
        return id;
    }

    public String getContent() {
        return content;
    }

    public String getVersion() {
        return version;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public long getLoadedAt() {
        return loadedAt;
    }

    public boolean isCritical() {
        return "critical".equalsIgnoreCase(riskLevel);
    }

    @Override
    public String toString() {
        return String.format("Prompt{id='%s', version='%s', riskLevel='%s'}", id, version, riskLevel);
    }
}
