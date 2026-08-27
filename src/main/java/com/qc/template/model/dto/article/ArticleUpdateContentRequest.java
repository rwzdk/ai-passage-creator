package com.qc.template.model.dto.article;

import lombok.Data;

import java.io.Serializable;

@Data
public class ArticleUpdateContentRequest implements Serializable {

    private String taskId;

    private String mainTitle;

    private String subTitle;

    private String content;

    private static final long serialVersionUID = 1L;
}
