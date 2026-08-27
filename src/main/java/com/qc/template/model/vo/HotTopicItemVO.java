package com.qc.template.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/** 单条热门选题。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HotTopicItemVO implements Serializable {

    private String title;

    private String source;

    private LocalDateTime publishedAt;

    private String url;

    private static final long serialVersionUID = 1L;
}
