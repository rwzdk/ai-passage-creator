package com.qc.template.service;

import com.qc.template.exception.BusinessException;
import com.qc.template.exception.ErrorCode;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.io.UnsupportedEncodingException;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class RegistrationEmailVerificationService {

    private static final long CODE_TTL_SECONDS = 5 * 60;
    private final JavaMailSender mailSender;
    private final String senderName;
    private final String senderAddress;
    private final ConcurrentHashMap<String, VerificationCode> codes = new ConcurrentHashMap<>();

    public RegistrationEmailVerificationService(JavaMailSender mailSender,
                                                @Value("${feedback.mail.sender-name}") String senderName,
                                                @Value("${spring.mail.username:}") String senderAddress) {
        this.mailSender = mailSender;
        this.senderName = senderName;
        this.senderAddress = senderAddress;
    }

    public void sendVerificationCode(String email) {
        String normalizedEmail = normalizeQqEmail(email);
        String code = String.format("%06d", ThreadLocalRandom.current().nextInt(1_000_000));
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
            helper.setTo(normalizedEmail);
            helper.setSubject("YuanJian Studio 注册验证码");
            if (!senderAddress.isBlank()) {
                helper.setFrom(senderAddress, senderName);
            }
            helper.setText("您的注册验证码为：" + code + "，5 分钟内有效。请勿将验证码告诉他人。", false);
            mailSender.send(message);
            codes.put(normalizedEmail, new VerificationCode(code, Instant.now().plusSeconds(CODE_TTL_SECONDS)));
        } catch (MailException | MessagingException | UnsupportedEncodingException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "验证码发送失败，请稍后重试");
        }
    }

    public void verifyAndConsume(String email, String code) {
        String normalizedEmail = normalizeQqEmail(email);
        VerificationCode verificationCode = codes.remove(normalizedEmail);
        if (verificationCode == null || Instant.now().isAfter(verificationCode.expiresAt())
                || code == null || !verificationCode.code().equals(code.trim())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "验证码错误或已过期");
        }
    }

    private String normalizeQqEmail(String email) {
        if (email == null || !email.trim().matches("^[A-Za-z0-9._%+-]+@qq\\.com$")) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请输入有效的 QQ 邮箱");
        }
        return email.trim().toLowerCase();
    }

    private record VerificationCode(String code, Instant expiresAt) {
    }
}
