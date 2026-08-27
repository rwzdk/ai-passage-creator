package com.qc.template.model.dto.article;

import lombok.Data;

import java.io.Serializable;

@Data
public class ArticleSelectImageVersionRequest implements Serializable {

    private String taskId;
    private Integer position;
    private String versionId;

    private static final long serialVersionUID = 1L;
}
