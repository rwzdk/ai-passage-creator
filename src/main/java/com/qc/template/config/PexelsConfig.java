package com.qc.template.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Pexels 閰嶇疆
 *
 */
@Configuration
@ConfigurationProperties(prefix = "pexels")
@Data
public class PexelsConfig {

    /**
     * API Key
     */
    private String apiKey;
}
