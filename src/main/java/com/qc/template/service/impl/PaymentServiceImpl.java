package com.qc.template.service.impl;

import com.mybatisflex.core.query.QueryWrapper;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.Refund;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.RefundCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import com.qc.template.config.StripeConfig;
import com.qc.template.constant.UserConstant;
import com.qc.template.exception.BusinessException;
import com.qc.template.exception.ErrorCode;
import com.qc.template.mapper.PaymentRecordMapper;
import com.qc.template.mapper.UserMapper;
import com.qc.template.model.entity.PaymentRecord;
import com.qc.template.model.entity.User;
import com.qc.template.model.enums.PaymentStatusEnum;
import com.qc.template.model.enums.ProductTypeEnum;
import com.qc.template.service.PaymentService;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 鏀粯鏈嶅姟瀹炵幇
 *
 */
@Service
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private static final String CURRENCY_USD = "usd";
    private static final long CENTS_MULTIPLIER = 100L;
    private static final String STATISTICS_CACHE_KEY = "statistics:overview";

    @Resource
    private StripeConfig stripeConfig;

    @Resource
    private UserMapper userMapper;

    @Resource
    private PaymentRecordMapper paymentRecordMapper;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public String createVipPaymentSession(Long userId) throws StripeException {
        User user = getUserOrThrow(userId);
        validateNotVip(user);

        ProductTypeEnum productType = ProductTypeEnum.VIP_PERMANENT;
        Session session = createStripeSession(userId, productType);
        savePaymentRecord(userId, session, productType);

        log.info("鍒涘缓鏀粯浼氳瘽鎴愬姛, userId={}, sessionId={}", userId, session.getId());
        return session.getUrl();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handlePaymentSuccess(Session session) {
        String sessionId = session.getId();
        String userId = session.getMetadata().get("userId");
        String paymentIntentId = session.getPaymentIntent();

        PaymentRecord record = findPaymentRecordBySessionId(sessionId);
        if (record == null) {
            log.warn("鏀粯璁板綍涓嶅瓨鍦? sessionId={}", sessionId);
            return;
        }

        // 骞傜瓑鎬ф鏌?
        if (PaymentStatusEnum.SUCCEEDED.getValue().equals(record.getStatus())) {
            log.info("鏀粯璁板綍宸插鐞? sessionId={}", sessionId);
            return;
        }

        updatePaymentStatus(record.getId(), PaymentStatusEnum.SUCCEEDED, paymentIntentId);
        upgradeUserToVip(Long.valueOf(userId));

        log.info("鏀粯鎴愬姛锛岀敤鎴峰凡鍗囩骇涓?VIP, userId={}, sessionId={}", userId, sessionId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean handleRefund(Long userId, String reason) throws StripeException {
        User user = getUserOrThrow(userId);
        validateIsVip(user);

        PaymentRecord paymentRecord = findLatestSuccessfulPayment(userId);
        if (paymentRecord == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "鏈壘鍒版敮浠樿褰?);
        }

        if (paymentRecord.getStripePaymentIntentId() == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "鏀粯璁板綍鏃犳晥");
        }

        Refund refund = createStripeRefund(paymentRecord.getStripePaymentIntentId());
        if (!"succeeded".equals(refund.getStatus())) {
            return false;
        }

        updateRefundRecord(paymentRecord.getId(), reason);
        revokeVipStatus(userId);

        log.info("閫€娆炬垚鍔燂紝宸插彇娑?VIP 韬唤, userId={}, refundId={}", userId, refund.getId());
        return true;
    }

    @Override
    public Event constructEvent(String payload, String sigHeader) throws Exception {
        return Webhook.constructEvent(payload, sigHeader, stripeConfig.getWebhookSecret());
    }

    @Override
    public List<PaymentRecord> getPaymentRecords(Long userId) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("userId", userId)
                .orderBy("createTime", false);
        return paymentRecordMapper.selectListByQuery(queryWrapper);
    }

    // ==================== 绉佹湁鏂规硶灏佽 ====================

    /**
     * 鑾峰彇鐢ㄦ埛鎴栨姏鍑哄紓甯?
     */
    private User getUserOrThrow(Long userId) {
        User user = userMapper.selectOneById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "鐢ㄦ埛涓嶅瓨鍦?);
        }
        return user;
    }

    /**
     * 楠岃瘉鐢ㄦ埛涓嶆槸 VIP
     */
    private void validateNotVip(User user) {
        if (UserConstant.VIP_ROLE.equals(user.getUserRole())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "鎮ㄥ凡缁忔槸姘镐箙浼氬憳");
        }
    }

    /**
     * 楠岃瘉鐢ㄦ埛鏄?VIP
     */
    private void validateIsVip(User user) {
        if (!UserConstant.VIP_ROLE.equals(user.getUserRole())
                && !UserConstant.ADMIN_ROLE.equals(user.getUserRole())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "鎮ㄤ笉鏄細鍛橈紝鏃犳硶閫€娆?);
        }
    }

    /**
     * 鍒涘缓 Stripe 鏀粯浼氳瘽
     */
    private Session createStripeSession(Long userId, ProductTypeEnum productType) throws StripeException {
        long amountInCents = productType.getPrice().multiply(new BigDecimal(CENTS_MULTIPLIER)).longValue();

        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(stripeConfig.getSuccessUrl())
                .setCancelUrl(stripeConfig.getCancelUrl())
                .addLineItem(buildLineItem(productType, amountInCents))
                .putMetadata("userId", String.valueOf(userId))
                .putMetadata("productType", productType.getValue())
                .build();

        return Session.create(params);
    }

    /**
     * 鏋勫缓鏀粯琛岄」鐩?
     */
    private SessionCreateParams.LineItem buildLineItem(ProductTypeEnum productType, long amountInCents) {
        return SessionCreateParams.LineItem.builder()
                .setPriceData(
                        SessionCreateParams.LineItem.PriceData.builder()
                                .setCurrency(CURRENCY_USD)
                                .setUnitAmount(amountInCents)
                                .setProductData(
                                        SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                .setName(productType.getDescription())
                                                .setDescription("解锁全部高级功能，无限创作配额，终身有效")
                                                .build()
                                )
                                .build()
                )
                .setQuantity(1L)
                .build();
    }

    /**
     * 淇濆瓨鏀粯璁板綍
     */
    private void savePaymentRecord(Long userId, Session session, ProductTypeEnum productType) {
        PaymentRecord record = PaymentRecord.builder()
                .userId(userId)
                .stripeSessionId(session.getId())
                .amount(productType.getPrice())
                .currency(CURRENCY_USD)
                .status(PaymentStatusEnum.PENDING.getValue())
                .productType(productType.getValue())
                .description(productType.getDescription())
                .build();
        paymentRecordMapper.insert(record);
    }

    /**
     * 鏍规嵁 Session ID 鏌ヨ鏀粯璁板綍
     */
    private PaymentRecord findPaymentRecordBySessionId(String sessionId) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("stripeSessionId", sessionId);
        return paymentRecordMapper.selectOneByQuery(queryWrapper);
    }

    /**
     * 鏌ヨ鏈€杩戠殑鎴愬姛鏀粯璁板綍
     */
    private PaymentRecord findLatestSuccessfulPayment(Long userId) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("userId", userId)
                .eq("status", PaymentStatusEnum.SUCCEEDED.getValue())
                .eq("productType", ProductTypeEnum.VIP_PERMANENT.getValue())
                .orderBy("createTime", false)
                .limit(1);
        return paymentRecordMapper.selectOneByQuery(queryWrapper);
    }

    /**
     * 鏇存柊鏀粯鐘舵€?
     */
    private void updatePaymentStatus(Long recordId, PaymentStatusEnum status, String paymentIntentId) {
        PaymentRecord updateRecord = new PaymentRecord();
        updateRecord.setId(recordId);
        updateRecord.setStatus(status.getValue());
        updateRecord.setStripePaymentIntentId(paymentIntentId);
        paymentRecordMapper.update(updateRecord);
    }

    /**
     * 鍗囩骇鐢ㄦ埛涓?VIP
     */
    private void upgradeUserToVip(Long userId) {
        User currentUser = getUserOrThrow(userId);
        User user = new User();
        user.setId(userId);
        user.setVipTime(LocalDateTime.now());
        if (!UserConstant.ADMIN_ROLE.equals(currentUser.getUserRole())) {
            user.setUserRole(UserConstant.VIP_ROLE);
        }
        userMapper.update(user);
        invalidateStatisticsCache();
    }

    /**
     * 鍒涘缓 Stripe 閫€娆?
     */
    private Refund createStripeRefund(String paymentIntentId) throws StripeException {
        RefundCreateParams params = RefundCreateParams.builder()
                .setPaymentIntent(paymentIntentId)
                .setReason(RefundCreateParams.Reason.REQUESTED_BY_CUSTOMER)
                .build();
        return Refund.create(params);
    }

    /**
     * 鏇存柊閫€娆捐褰?
     */
    private void updateRefundRecord(Long recordId, String reason) {
        PaymentRecord updateRecord = new PaymentRecord();
        updateRecord.setId(recordId);
        updateRecord.setStatus(PaymentStatusEnum.REFUNDED.getValue());
        updateRecord.setRefundTime(LocalDateTime.now());
        updateRecord.setRefundReason(reason);
        paymentRecordMapper.update(updateRecord);
    }

    /**
     * 鎾ら攢鐢ㄦ埛 VIP 韬唤
     */
    private void revokeVipStatus(Long userId) {
        User currentUser = getUserOrThrow(userId);
        User updateUser = new User();
        updateUser.setId(userId);
        updateUser.setVipTime(null);
        if (!UserConstant.ADMIN_ROLE.equals(currentUser.getUserRole())) {
            updateUser.setUserRole(UserConstant.DEFAULT_ROLE);
            updateUser.setQuota(UserConstant.DEFAULT_QUOTA);
        }
        userMapper.update(updateUser);
        invalidateStatisticsCache();
    }

    /** 鐢ㄦ埛浼氬憳鐘舵€佸彉鍖栧悗锛岀珛鍗冲埛鏂扮鐞嗙缁熻鏁版嵁銆?*/
    private void invalidateStatisticsCache() {
        redisTemplate.delete(STATISTICS_CACHE_KEY);
    }
}
