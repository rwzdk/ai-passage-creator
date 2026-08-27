package com.qc.template.model.enums;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 鏂囩珷椋庢牸鏋氫妇
 *
 */
@Getter
public enum ArticleStyleEnum {

    TECH("tech", "绉戞妧椋庢牸"),
    EMOTIONAL("emotional", "鎯呮劅椋庢牸"),
    EDUCATIONAL("educational", "鏁欒偛椋庢牸"),
    HUMOROUS("humorous", "杞绘澗骞介粯椋庢牸");

    private final String value;
    private final String text;

    ArticleStyleEnum(String value, String text) {
        this.value = value;
        this.text = text;
    }

    /**
     * 鑾峰彇鎵€鏈夊€煎垪琛?
     */
    public static List<String> getValues() {
        return Arrays.stream(values())
                .map(ArticleStyleEnum::getValue)
                .collect(Collectors.toList());
    }

    /**
     * 鏍规嵁 value 鑾峰彇鏋氫妇
     */
    public static ArticleStyleEnum getEnumByValue(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        for (ArticleStyleEnum styleEnum : ArticleStyleEnum.values()) {
            if (styleEnum.getValue().equals(value)) {
                return styleEnum;
            }
        }
        return null;
    }

    /**
     * 鏍￠獙鏄惁涓烘湁鏁堢殑椋庢牸鍊?
     */
    public static boolean isValid(String value) {
        if (value == null || value.isEmpty()) {
            return true; // 鍏佽涓虹┖
        }
        return getEnumByValue(value) != null;
    }
}
