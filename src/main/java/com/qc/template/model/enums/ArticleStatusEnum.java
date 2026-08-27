package com.qc.template.model.enums;

import lombok.Getter;

/**
 * 鏂囩珷鐘舵€佹灇涓?
 *
 */
@Getter
public enum ArticleStatusEnum {

    PENDING("PENDING", "绛夊緟澶勭悊"),
    PROCESSING("PROCESSING", "澶勭悊涓?),
    COMPLETED("COMPLETED", "宸插畬鎴?),
    FAILED("FAILED", "澶辫触");

    /**
     * 鐘舵€佸€?
     */
    private final String value;

    /**
     * 鐘舵€佹弿杩?
     */
    private final String description;

    ArticleStatusEnum(String value, String description) {
        this.value = value;
        this.description = description;
    }

    /**
     * 鏍规嵁鍊艰幏鍙栨灇涓?
     *
     * @param value 鐘舵€佸€?
     * @return 鏋氫妇瀹炰緥
     */
    public static ArticleStatusEnum getByValue(String value) {
        if (value == null) {
            return null;
        }
        for (ArticleStatusEnum statusEnum : values()) {
            if (statusEnum.getValue().equals(value)) {
                return statusEnum;
            }
        }
        return null;
    }
}
