package com.qc.template.controller;

import com.qc.template.annotation.AuthCheck;
import com.qc.template.common.BaseResponse;
import com.qc.template.common.ResultUtils;
import com.qc.template.constant.UserConstant;
import com.qc.template.exception.BusinessException;
import com.qc.template.exception.ErrorCode;
import com.qc.template.model.entity.PaymentRecord;
import com.qc.template.model.entity.User;
import com.qc.template.service.PaymentService;
import com.qc.template.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 鏀粯鎺у埗鍣?
 *
 */
@RestController
@RequestMapping("/payment")
@Slf4j
@Tag(name = "PaymentController", description = "鏀粯鎺ュ彛")
public class PaymentController {

    @Resource
    private PaymentService paymentService;

    @Resource
    private UserService userService;

    /**
     * 鍒涘缓 VIP 鏀粯浼氳瘽
     */
    @PostMapping("/create-vip-session")
    @Operation(summary = "鍒涘缓 VIP 鏀粯浼氳瘽")
    public BaseResponse<String> createVipPaymentSession(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        try {
            String sessionUrl = paymentService.createVipPaymentSession(loginUser.getId());
            return ResultUtils.success(sessionUrl);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("鍒涘缓鏀粯浼氳瘽澶辫触", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "鍒涘缓鏀粯浼氳瘽澶辫触");
        }
    }

    /**
     * 鐢宠閫€娆?
     */
    @PostMapping("/refund")
    @Operation(summary = "鐢宠閫€娆?)
    @AuthCheck(mustRole = UserConstant.VIP_ROLE)
    public BaseResponse<Boolean> refund(
            @RequestParam(required = false) String reason,
            HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        try {
            boolean success = paymentService.handleRefund(loginUser.getId(), reason);
            return ResultUtils.success(success);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("閫€娆惧け璐?, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "閫€娆惧け璐?);
        }
    }

    /**
     * 鑾峰彇褰撳墠鐢ㄦ埛鏀粯璁板綍
     */
    @GetMapping("/records")
    @Operation(summary = "鑾峰彇褰撳墠鐢ㄦ埛鏀粯璁板綍")
    public BaseResponse<List<PaymentRecord>> getPaymentRecords(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        List<PaymentRecord> records = paymentService.getPaymentRecords(loginUser.getId());
        return ResultUtils.success(records);
    }
}
