package com.qc.template.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/** 当前用户的创作统计。 */
@Data
public class UserArticleStatsVO implements Serializable {

    private Long totalWorks;
    private Long completedWorks;
    private Long totalCharacters;
    private LocalDateTime latestWorkTime;

    private static final long serialVersionUID = 1L;
}
