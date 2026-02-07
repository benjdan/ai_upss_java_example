package com.upss.core;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class PromptLoader {
    private static final Logger logger = LoggerFactory.getLogger(PromptLoader.class);
    private final String configPath;
    private final boolean enableValidation;
    private final boolean requireChecksum;
    private JsonObject configuration;

    public PromptLoader(String configPath, boolean enableValidation, boolean requireChecksum) {
        this.configPath = configPath;
        this.enableValidation = enableValidation;
        this.requireChecksum = requireChecksum;
        loadConfiguration();
    }

    public PromptLoader(String configPath) {
        this(configPath, true, true);
    }

    private void loadConfiguration() {
        try {
            File configFile = new File(configPath);
            if (!configFile.exists()) {
                logger.warn("Configuration file not found at: {}", configPath);
                this.configuration = new JsonObject();
                return;
            }

            FileReader reader = new FileReader(configFile);
            this.configuration = JsonParser.parseReader(reader).getAsJsonObject();
            reader.close();
            logger.info("Configuration loaded successfully from: {}", configPath);
        } catch (IOException | IllegalStateException e) {
            logger.error("Failed to load configuration", e);
            this.configuration = new JsonObject();
        }
    }

    public Prompt loadPrompt(String promptId) throws PromptLoadException {
        logger.debug("Loading prompt: {}", promptId);

        if (!configuration.has("prompts")) {
            throw new PromptLoadException("No prompts defined in configuration");
        }

        JsonObject prompts = configuration.getAsJsonObject("prompts");
        if (!prompts.has(promptId)) {
            throw new PromptLoadException("Prompt not found: " + promptId);
        }

        JsonObject promptConfig = prompts.getAsJsonObject(promptId);
        String path = promptConfig.get("path").getAsString();
        String version = promptConfig.get("version").getAsString();
        String riskLevel = promptConfig.has("riskLevel") ? promptConfig.get("riskLevel").getAsString() : "medium";
        String expectedChecksum = promptConfig.has("checksum") ? promptConfig.get("checksum").getAsString() : null;

        // Load prompt content
        String content = loadPromptFile(path);

        if (requireChecksum && expectedChecksum != null) {
            String actualChecksum = calculateChecksum(content);
            if (!actualChecksum.equals(expectedChecksum)) {
                logger.error("Checksum mismatch for prompt: {}", promptId);
                throw new PromptLoadException("Checksum verification failed for: " + promptId);
            }
            logger.debug("Checksum verified for prompt: {}", promptId);
        }

        logger.info("Successfully loaded prompt: {} (version: {})", promptId, version);
        return new Prompt(promptId, content, version, riskLevel);
    }

    private String loadPromptFile(String relativePath) throws PromptLoadException {
        try {
            File configDir = new File(configPath).getParentFile();
            File promptFile = new File(configDir, relativePath);

            if (!promptFile.exists()) {
                throw new PromptLoadException("Prompt file not found: " + promptFile.getAbsolutePath());
            }
            
            StringBuilder content = new StringBuilder();
            try (FileReader reader = new FileReader(promptFile)) {
                int c;
                while ((c = reader.read()) != -1) {
                    content.append((char) c);
                }
            }

            return content.toString();
        } catch (IOException e) {
        	logger.error("Failed to load prompt file", e);
            throw new PromptLoadException("Failed to load prompt file", e);
        }
    }

    public String calculateChecksum(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return "sha256:" + hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            logger.error("SHA-256 algorithm not available", e);
            throw new RuntimeException("Checksum calculation failed", e);
        }
    }

    public static class PromptLoadException extends Exception {
        public PromptLoadException(String message) {
            super(message);
        }

        public PromptLoadException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
