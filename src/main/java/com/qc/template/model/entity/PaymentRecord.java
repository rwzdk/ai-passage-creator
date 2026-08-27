package com.qc.template.model.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 鏀粯璁板綍瀹炰綋绫?
 *
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(value = "payment_record", camelToUnderline = false)
public class PaymentRecord implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    @Id(keyType = KeyType.Auto)
    private Long id;

    /**
     * 鐢ㄦ埛ID
     */
    private Long userId;

    /**
     * Stripe Checkout Session ID
     */
    private String stripeSessionId;

    /**
     * Stripe 鏀粯鎰忓悜ID
     */
    private String stripePaymentIntentId;

    /**
     * 金额（美元）
     */
    private BigDecimal amount;

    /**
     * 璐у竵
     */
    private String currency;

    /**
     * 鐘舵€侊細PENDING/SUCCEEDED/FAILED/REFUNDED
     */
    private String status;

    /**
     * 产品类型：VIP_PERMANENT
     */
    private String productType;

    /**
     * 鎻忚堪
     */
    private String description;

    /**
     * 閫€娆炬椂闂?
     */
    private LocalDateTime refundTime;

    /**
     * 閫€娆惧師鍥?
     */
    private String refundReason;

    /**
     * 鍒涘缓鏃堕棿
     */
    private LocalDateTime createTime;

    /**
     * 鏇存柊鏃堕棿
     */
    private LocalDateTime updateTime;
}
