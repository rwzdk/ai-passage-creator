package com.yupi.template.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "nano-banana-apiclaude")
@Data
public class NanoBananaApiclaudeConfig {
    private String apiKey;
    private String baseUrl = "https://apiclaude.cc";
    private String model = "gemini-3-pro-image-preview";
    private String aspectRatio = "16:9";
    private String imageSize = "1K";
}
