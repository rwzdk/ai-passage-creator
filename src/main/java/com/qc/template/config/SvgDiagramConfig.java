package com.qc.template.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import static com.qc.template.constant.ArticleConstant.*;

/**
 * SVG 姒傚康绀烘剰鍥剧敓鎴愰厤缃?
 *
 */
@Configuration
@ConfigurationProperties(prefix = "svg-diagram")
@Data
public class SvgDiagramConfig {

    /**
     * 榛樿瀹藉害
     */
    private Integer defaultWidth = SVG_DEFAULT_WIDTH;

    /**
     * 榛樿楂樺害
     */
    private Integer defaultHeight = SVG_DEFAULT_HEIGHT;

    /**
     * COS 瀛樺偍鏂囦欢澶?
     */
    private String folder = "svg-diagrams";
}
