package com.qc.template.service;

import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.qc.template.model.entity.PaymentRecord;

import java.util.List;

/**
 * 鏀粯鏈嶅姟
 *
 */
public interface PaymentService {

    /**
     * 鍒涘缓 VIP 姘镐箙浼氬憳鏀粯浼氳瘽
     *
     * @param userId 鐢ㄦ埛ID
     * @return Stripe Checkout Session URL
     */
    String createVipPaymentSession(Long userId) throws StripeException;

    /**
     * 澶勭悊鏀粯鎴愬姛鍥炶皟
     *
     * @param session Stripe Checkout Session
     */
    void handlePaymentSuccess(Session session);

    /**
     * 澶勭悊閫€娆?
     *
     * @param userId 鐢ㄦ埛ID
     * @param reason 閫€娆惧師鍥?
     * @return 鏄惁閫€娆炬垚鍔?
     */
    boolean handleRefund(Long userId, String reason) throws StripeException;

    /**
     * 楠岃瘉 Webhook 绛惧悕
     *
     * @param payload 璇锋眰浣?
     * @param sigHeader 绛惧悕澶?
     * @return Stripe Event
     */
    Event constructEvent(String payload, String sigHeader) throws Exception;

    /**
     * 鑾峰彇鐢ㄦ埛鏀粯璁板綍
     *
     * @param userId 鐢ㄦ埛ID
     * @return 鏀粯璁板綍鍒楄〃
     */
    List<PaymentRecord> getPaymentRecords(Long userId);
}
