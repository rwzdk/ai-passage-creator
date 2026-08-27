package com.qc.template.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Mermaid 鍥捐〃鐢熸垚閰嶇疆
 *
 */
@Configuration
@ConfigurationProperties(prefix = "mermaid")
@Data
public class MermaidConfig {

    /**
     * CLI 鍛戒护锛圵indows 涓嬩负 mmdc.cmd锛孡inux/Mac 涓嬩负 mmdc锛?
     */
    private String cliCommand = "mmdc";

    /**
     * 鑳屾櫙棰滆壊锛坱ransparent 涓洪€忔槑鑳屾櫙锛?
     */
    private String backgroundColor = "transparent";

    /**
     * 杈撳嚭鏍煎紡锛坰vg/png/pdf锛?
     */
    private String outputFormat = "svg";

    /**
     * 图片宽度（像素）
     */
    private Integer width = 800;

    /**
     * 命令执行超时时间（毫秒）
     */
    private Long timeout = 30000L;
}
