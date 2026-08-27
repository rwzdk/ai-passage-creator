package com.qc.template.model.dto.article;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 纭澶х翰璇锋眰
 *
 */
@Data
public class ArticleConfirmOutlineRequest implements Serializable {

    /**
     * 浠诲姟ID
     */
    private String taskId;

    /**
     * 鐢ㄦ埛缂栬緫鍚庣殑澶х翰
     */
    private List<ArticleState.OutlineSection> outline;

    private static final long serialVersionUID = 1L;
}
