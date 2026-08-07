package com.yupi.template.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/** 热门选题列表。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HotTopicsVO implements Serializable {

    private String source;

    private LocalDateTime updatedAt;

    private List<HotTopicItemVO> items;

    private static final long serialVersionUID = 1L;
}
