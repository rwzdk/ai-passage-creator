package com.qc.template.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 文档参考材料解析结果。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentReferenceVO implements Serializable {

    private String fileName;

    private String summary;

    private int characterCount;

    private static final long serialVersionUID = 1L;
}
