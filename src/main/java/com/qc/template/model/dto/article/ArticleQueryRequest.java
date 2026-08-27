package com.qc.template.model.dto.article;

import com.qc.template.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 鏌ヨ鏂囩珷璇锋眰
 *
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ArticleQueryRequest extends PageRequest implements Serializable {

    /**
     * 鐢ㄦ埛ID
     */
    private Long userId;

    /**
     * 鐘舵€?
     */
    private String status;

    private static final long serialVersionUID = 1L;
}
