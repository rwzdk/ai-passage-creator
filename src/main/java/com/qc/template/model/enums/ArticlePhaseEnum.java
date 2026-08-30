package com.qc.template.model.enums;

import lombok.Getter;

@Getter
public enum ArticlePhaseEnum {
    PENDING("PENDING", "等待处理"),
    TITLE_GENERATING("TITLE_GENERATING", "生成标题中"),
    TITLE_SELECTING("TITLE_SELECTING", "等待选择标题"),
    OUTLINE_GENERATING("OUTLINE_GENERATING", "生成大纲中"),
    OUTLINE_EDITING("OUTLINE_EDITING", "等待编辑大纲"),
    CONTENT_GENERATING("CONTENT_GENERATING", "生成正文中");

    private final String value;
    private final String description;

    ArticlePhaseEnum(String value, String description) {
        this.value = value;
        this.description = description;
    }

    public static ArticlePhaseEnum getByValue(String value) {
        if (value == null) {
            return null;
        }
        for (ArticlePhaseEnum phaseEnum : values()) {
            if (phaseEnum.getValue().equals(value)) {
                return phaseEnum;
            }
        }
        return null;
    }

    public boolean canTransitionTo(ArticlePhaseEnum targetPhase) {
        if (targetPhase == null) {
            return false;
        }
        return switch (this) {
            case PENDING -> targetPhase == TITLE_GENERATING;
            case TITLE_GENERATING -> targetPhase == TITLE_SELECTING;
            case TITLE_SELECTING -> targetPhase == OUTLINE_GENERATING;
            case OUTLINE_GENERATING -> targetPhase == OUTLINE_EDITING;
            case OUTLINE_EDITING -> targetPhase == CONTENT_GENERATING;
            case CONTENT_GENERATING -> false;
        };
    }
}
