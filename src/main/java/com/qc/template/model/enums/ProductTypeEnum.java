package com.qc.template.model.enums;

import lombok.Getter;

import java.math.BigDecimal;

/**
 * 浜у搧绫诲瀷鏋氫妇
 *
 */
@Getter
public enum ProductTypeEnum {

    VIP_PERMANENT("VIP_PERMANENT", "姘镐箙浼氬憳", new BigDecimal("199.00"));

    private final String value;
    private final String description;
    private final BigDecimal price;

    ProductTypeEnum(String value, String description, BigDecimal price) {
        this.value = value;
        this.description = description;
        this.price = price;
    }

    public static ProductTypeEnum getByValue(String value) {
        if (value == null) {
            return null;
        }
        for (ProductTypeEnum typeEnum : values()) {
            if (typeEnum.getValue().equals(value)) {
                return typeEnum;
            }
        }
        return null;
    }
}
