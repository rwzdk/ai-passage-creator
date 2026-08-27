package com.qc.template.controller;

import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.qc.template.service.PaymentService;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * Stripe Webhook 鎺у埗鍣?
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
     * 澶勭悊 Stripe Webhook 鍥炶皟
     */
    @PostMapping("/stripe")
    public String handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {
        
        try {
            // 楠岃瘉 Webhook 绛惧悕
            Event event = paymentService.constructEvent(payload, sigHeader);
            
            log.info("鏀跺埌 Stripe Webhook 浜嬩欢, type={}", event.getType());
            
            // 澶勭悊浜嬩欢
            switch (event.getType()) {
                case "checkout.session.completed":
                    // 鏀粯鎴愬姛
                    JsonObject sessionJson = JsonParser.parseString(
                            event.getDataObjectDeserializer().getRawJson()).getAsJsonObject();
                    String sessionId = sessionJson.get("id").getAsString();
                    Session session = Session.retrieve(sessionId);
                    paymentService.handlePaymentSuccess(session);
                    break;
                    
                case "checkout.session.async_payment_succeeded":
                    // 寮傛鏀粯鎴愬姛
                    JsonObject asyncSessionJson = JsonParser.parseString(
                            event.getDataObjectDeserializer().getRawJson()).getAsJsonObject();
                    String asyncSessionId = asyncSessionJson.get("id").getAsString();
                    Session asyncSession = Session.retrieve(asyncSessionId);
                    paymentService.handlePaymentSuccess(asyncSession);
                    break;
                    
                default:
                    log.info("鏈鐞嗙殑浜嬩欢绫诲瀷: {}", event.getType());
                    break;
            }
            
            return "success";
        } catch (Exception e) {
            log.error("澶勭悊 Stripe Webhook 澶辫触", e);
            return "error";
        }
    }
}
