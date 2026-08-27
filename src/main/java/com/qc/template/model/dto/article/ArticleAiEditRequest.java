package com.qc.template.model.dto.article;

import lombok.Data;

import java.io.Serializable;

@Data
public class ArticleAiEditRequest implements Serializable {

    private String taskId;

    private String content;

    private String instruction;

    private static final long serialVersionUID = 1L;
}
