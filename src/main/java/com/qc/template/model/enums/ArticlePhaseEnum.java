package com.qc.template.model.enums;

import lombok.Getter;

/**
 * 鏂囩珷闃舵鏋氫妇
 *
 */
@Getter
public enum ArticlePhaseEnum {

    PENDING("PENDING", "绛夊緟澶勭悊"),
    TITLE_GENERATING("TITLE_GENERATING", "鐢熸垚鏍囬涓?),
    TITLE_SELECTING("TITLE_SELECTING", "绛夊緟閫夋嫨鏍囬"),
    OUTLINE_GENERATING("OUTLINE_GENERATING", "鐢熸垚澶х翰涓?),
    OUTLINE_EDITING("OUTLINE_EDITING", "绛夊緟缂栬緫澶х翰"),
    CONTENT_GENERATING("CONTENT_GENERATING", "鐢熸垚姝ｆ枃涓?);

    /**
     * 闃舵鍊?
     */
    private final String value;

    /**
     * 闃舵鎻忚堪
     */
    private final String description;

    ArticlePhaseEnum(String value, String description) {
        this.value = value;
        this.description = description;
    }

    /**
     * 鏍规嵁鍊艰幏鍙栨灇涓?
     *
     * @param value 闃舵鍊?
     * @return 鏋氫妇瀹炰緥
     */
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

    /**
     * 鏍￠獙鏄惁鍙互杞崲鍒扮洰鏍囬樁娈?
     *
     * @param targetPhase 鐩爣闃舵
     * @return 鏄惁鍙互杞崲
     */
    public boolean canTransitionTo(ArticlePhaseEnum targetPhase) {
        if (targetPhase == null) {
            return false;
        }
        
        // 瀹氫箟鍚堟硶鐨勭姸鎬佽浆鎹?
        return switch (this) {
            case PENDING -> targetPhase == TITLE_GENERATING;
            case TITLE_GENERATING -> targetPhase == TITLE_SELECTING;
            case TITLE_SELECTING -> targetPhase == OUTLINE_GENERATING;
            case OUTLINE_GENERATING -> targetPhase == OUTLINE_EDITING;
            case OUTLINE_EDITING -> targetPhase == CONTENT_GENERATING;
            case CONTENT_GENERATING -> false; // 鏈€缁堥樁娈碉紝涓嶅啀杞崲
        };
    }
}
