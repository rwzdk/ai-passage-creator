package com.qc.template.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import static com.qc.template.constant.ArticleConstant.*;

/**
 * 琛ㄦ儏鍖呮绱㈤厤缃?
 *
 */
@Configuration
@ConfigurationProperties(prefix = "emoji-pack")
@Data
public class EmojiPackConfig {

    /**
     * Bing 鍥剧墖鎼滅储鍦板潃
     */
    private String searchUrl = BING_IMAGE_SEARCH_URL;

    /**
     * 琛ㄦ儏鍖呭叧閿瘝鍚庣紑锛堢▼搴忓浐瀹氭嫾鎺ワ紝涓嶄緷璧?AI 杩斿洖锛?
     */
    private String suffix = EMOJI_PACK_SUFFIX;

    /**
     * 璇锋眰瓒呮椂鏃堕棿锛堟绉掞級
     */
    private Integer timeout = 10000;
}
