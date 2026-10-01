package com.osx23.avi.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "avi.whisper")
public record WhisperProperties(
        String command,
        String model,
        String language,
        String device,
        Integer timeoutSeconds
) {
    public String effectiveCommand() {
        return command == null || command.isBlank() ? "whisper" : command;
    }

    public String effectiveModel() {
        return model == null || model.isBlank() ? "small" : model;
    }

    public String effectiveLanguage() {
        return language == null || language.isBlank() ? "Spanish" : language;
    }

    public String effectiveDevice() {
        return device == null ? "" : device.trim();
    }

    public int effectiveTimeoutSeconds() {
        return timeoutSeconds == null || timeoutSeconds < 10 ? 600 : timeoutSeconds;
    }
}
