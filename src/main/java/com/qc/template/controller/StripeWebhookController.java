package com.qc.template.controller;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.qc.template.service.PaymentService;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Stripe Webhook 控制器
 *
 */
@RestController
@RequestMapping("/webhook")
@Slf4j
@Hidden
public class StripeWebhookController {

    @Resource
    private PaymentService paymentService;

    /**
     * 处理 Stripe Webhook 鍥炶皟
     */
    @PostMapping("/stripe")
    public ResponseEntity<String> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {
        
        try {
            // 验证 Webhook 签名
            Event event = paymentService.constructEvent(payload, sigHeader);
            
            log.info("鏀跺埌 Stripe Webhook 浜嬩欢, type={}", event.getType());
            
            // 处理事件
            switch (event.getType()) {
                case "checkout.session.completed":
                    // 支付成功
                    JsonObject sessionJson = JsonParser.parseString(
                            event.getDataObjectDeserializer().getRawJson()).getAsJsonObject();
                    String sessionId = sessionJson.get("id").getAsString();
                    Session session = Session.retrieve(sessionId);
                    paymentService.handlePaymentSuccess(session);
                    break;
                    
                case "checkout.session.async_payment_succeeded":
                    // 异步支付成功
                    JsonObject asyncSessionJson = JsonParser.parseString(
                            event.getDataObjectDeserializer().getRawJson()).getAsJsonObject();
                    String asyncSessionId = asyncSessionJson.get("id").getAsString();
                    Session asyncSession = Session.retrieve(asyncSessionId);
                    paymentService.handlePaymentSuccess(asyncSession);
                    break;
                    
                default:
                    log.info("未处理的事件类型: {}", event.getType());
                    break;
            }
            
            return ResponseEntity.ok("success");
        } catch (SignatureVerificationException e) {
            log.warn("Stripe Webhook signature verification failed", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("invalid signature");
        } catch (Exception e) {
            log.error("处理 Stripe Webhook 失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("error");
        }
    }
}
