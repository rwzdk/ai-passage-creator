package com.yupi.template.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * GPT Image 2 配置。
 */
@Configuration
@ConfigurationProperties(prefix = "image-2")
@Data
public class Image2Config {

    private String apiKey;
    private String baseUrl = "https://apiclaude.cc";
    private String model = "gpt-image-2";
    private String size = "1024x1024";
    private int n = 1;
}
