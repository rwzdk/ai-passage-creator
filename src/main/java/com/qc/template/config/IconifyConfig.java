package com.qc.template.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Iconify 鍥炬爣搴撻厤缃?
 *
 */
@Configuration
@ConfigurationProperties(prefix = "iconify")
@Data
public class IconifyConfig {

    /**
     * Iconify API 鍦板潃
     */
    private String apiUrl = "https://api.iconify.design";

    /**
     * 鎼滅储缁撴灉闄愬埗鏁伴噺
     */
    private Integer searchLimit = 10;

    /**
     * 榛樿鍥炬爣楂樺害锛堝儚绱狅級
     */
    private Integer defaultHeight = 64;

    /**
     * 榛樿鍥炬爣棰滆壊锛堢暀绌轰娇鐢?currentColor锛屾垨璁剧疆濡?"#000000"锛?
     */
    private String defaultColor = "";
}
