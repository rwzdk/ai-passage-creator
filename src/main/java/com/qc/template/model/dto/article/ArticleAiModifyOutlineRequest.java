package com.qc.template.model.dto.article;

import lombok.Data;

import java.io.Serializable;

/**
 * AI 淇敼澶х翰璇锋眰
 *
 */
@Data
public class ArticleAiModifyOutlineRequest implements Serializable {

    /**
     * 浠诲姟ID
     */
    private String taskId;

    /**
     * 鐢ㄦ埛鐨勪慨鏀瑰缓璁?
     */
    private String modifySuggestion;

    private static final long serialVersionUID = 1L;
}
