package com.learningplatform.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        Cors cors,
        Storage storage,
        Auth auth
) {
    public record Cors(List<String> allowedOrigins) {
    }

    public record Storage(
            String endpoint,
            String accessKey,
            String secretKey,
            String bucket,
            String region,
            String localBasePath,
            long maxFileBytes
    ) {
        public long effectiveMaxFileBytes() {
            return maxFileBytes > 0 ? maxFileBytes : 50L * 1024 * 1024;
        }

        public String effectiveLocalBasePath() {
            return localBasePath != null && !localBasePath.isBlank() ? localBasePath : "./data/storage";
        }
    }

    public record Auth(boolean mockEnabled) {
    }
}
