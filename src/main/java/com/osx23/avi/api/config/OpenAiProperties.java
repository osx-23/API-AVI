package com.osx23.avi.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "avi.openai")
public record OpenAiProperties(
        String apiKey,
        String transcriptionModel,
        String baseUrl
) {
    public boolean configured() {
        return apiKey != null && !apiKey.isBlank();
    }

    public String model() {
        return transcriptionModel == null || transcriptionModel.isBlank()
                ? "gpt-4o-mini-transcribe"
                : transcriptionModel;
    }

    public String endpoint() {
        String root = baseUrl == null || baseUrl.isBlank()
                ? "https://api.openai.com/v1"
                : baseUrl.replaceAll("/+$", "");
        return root + "/audio/transcriptions";
    }
}
