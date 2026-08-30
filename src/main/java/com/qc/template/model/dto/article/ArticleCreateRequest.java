package com.qc.template.model.dto.article;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 创建文章请求
 *
 */
@Data
public class ArticleCreateRequest implements Serializable {

    /**
     * 选题
     */
    private String topic;

    /**
     * 文章风格：tech/emotional/educational/humorous，可为空
     */
    private String style;

    /**
     * 允许的配图方式列表（为空或 null 表示支持所有方式）
     * 可：PEXELS, NANO_BANANA, MERMAID, ICONIFY, EMOJI_PACK, SVG_DIAGRAM
     */
    private List<String> enabledImageMethods;

    /**
     * 上传文档生成的参考摘要，可为空
     */
    private String referenceSummary;

    private static final long serialVersionUID = 1L;
}
