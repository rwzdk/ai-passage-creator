package com.yupi.template.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** GNews 热门选题配置。 */
@Configuration
@ConfigurationProperties(prefix = "gnews")
@Data
public class GNewsConfig {

    private String apiKey;

    private String baseUrl = "https://gnews.io/api/v4";

    private int cacheMinutes = 10;

    private int maxItems = 8;
}
