package com.qc.template.model.enums;

import lombok.Getter;

/**
 * 鏀粯鐘舵€佹灇涓?
 *
 */
@Getter
public enum PaymentStatusEnum {

    PENDING("PENDING", "寰呮敮浠?),
    SUCCEEDED("SUCCEEDED", "鏀粯鎴愬姛"),
    FAILED("FAILED", "鏀粯澶辫触"),
    REFUNDED("REFUNDED", "宸查€€娆?);

    private final String value;
    private final String description;

    PaymentStatusEnum(String value, String description) {
        this.value = value;
        this.description = description;
    }

    public static PaymentStatusEnum getByValue(String value) {
        if (value == null) {
            return null;
        }
        for (PaymentStatusEnum statusEnum : values()) {
            if (statusEnum.getValue().equals(value)) {
                return statusEnum;
            }
        }
        return null;
    }
}
