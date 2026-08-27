package com.qc.template.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 鑵捐浜?COS 閰嶇疆
 *
 */
@Configuration
@ConfigurationProperties(prefix = "tencent.cos")
@Data
public class CosConfig {

    /**
     * Secret ID
     */
    private String secretId;

    /**
     * Secret Key
     */
    private String secretKey;

    /**
     * 鍦板煙
     */
    private String region;

    /**
     * 瀛樺偍妗?
     */
    private String bucket;
}
