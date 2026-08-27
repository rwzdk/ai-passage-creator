package com.qc.template.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Nano Banana (Gemini 鍘熺敓鍥剧墖鐢熸垚) 閰嶇疆
 *
 */
@Configuration
@ConfigurationProperties(prefix = "nano-banana")
@Data
public class NanoBananaConfig {

    /**
     * Gemini API Key
     */
    private String apiKey;


    /**
     * 妯″瀷鍚嶇О
     * gemini-2.5-flash-image: 閫熷害蹇紝閫傚悎楂樺悶鍚愪綆寤惰繜
     * gemini-3-pro-image-preview: 涓撲笟绾э紝鏀寔楂樼骇鎺ㄧ悊鍜岄珮鍒嗚鲸鐜?
     */
    private String model = "gemini-2.5-flash-image";

    /**
     * 鍥剧墖瀹介珮姣?
     * 鏀寔: 1:1, 2:3, 3:2, 3:4, 4:3, 4:5, 5:4, 9:16, 16:9, 21:9
     */
    private String aspectRatio = "16:9";

    /**
     * 鍥剧墖鍒嗚鲸鐜囷紙浠?gemini-3-pro-image-preview 鏀寔锛?
     * 鏀寔: 1K, 2K, 4K
     */
    private String imageSize = "1K";

    /**
     * 杈撳嚭鍥剧墖鏍煎紡
     * 鏀寔: image/jpeg, image/png
     */
    private String outputMimeType = "image/png";
}
