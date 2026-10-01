package com.osx23.avi.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "avi.supabase")
public record SupabaseProperties(
        String url,
        String key
) {
    public boolean configured() {
        return url != null && !url.isBlank() && key != null && !key.isBlank();
    }
}
