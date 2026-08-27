package com.example.base_api.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Arrays;
import java.util.List;

@ConfigurationProperties(prefix = "app.cors")
public class CorsProperties {

    private String allowedOriginPatterns = "http://localhost:*,http://127.0.0.1:*";

    public String getAllowedOriginPatterns() {
        return allowedOriginPatterns;
    }

    public void setAllowedOriginPatterns(String allowedOriginPatterns) {
        this.allowedOriginPatterns = allowedOriginPatterns;
    }

    public List<String> resolvedPatterns() {
        if (allowedOriginPatterns == null || allowedOriginPatterns.isBlank()) {
            return List.of("http://localhost:*", "http://127.0.0.1:*");
        }
        List<String> origens = Arrays.stream(allowedOriginPatterns.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty() && !"*".equals(s))
                .toList();
        return origens.isEmpty() ? List.of("http://localhost:*", "http://127.0.0.1:*") : origens;
    }
}
