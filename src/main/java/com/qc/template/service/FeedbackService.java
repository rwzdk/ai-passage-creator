package com.qc.template.service;

import com.qc.template.exception.BusinessException;
import com.qc.template.exception.ErrorCode;
import com.qc.template.model.entity.User;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class FeedbackService {

    private static final int MAX_CONTENT_LENGTH = 1000;
    private static final int MAX_IMAGE_COUNT = 3;
    private static final long MAX_IMAGE_SIZE = 5L * 1024 * 1024;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");

    private final JavaMailSender mailSender;
    private final String recipient;
    private final String senderName;
    private final String senderAddress;

    public FeedbackService(JavaMailSender mailSender,
                           @Value("${feedback.mail.recipient}") String recipient,
                           @Value("${feedback.mail.sender-name}") String senderName,
                           @Value("${spring.mail.username:}") String senderAddress) {
        this.mailSender = mailSender;
        this.recipient = recipient;
        this.senderName = senderName;
        this.senderAddress = senderAddress;
    }

    public void sendFeedback(User currentUser, String content, List<MultipartFile> images) {
        validate(currentUser, content, images);
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            helper.setTo(recipient);
            helper.setSubject("创作平台反馈建议");
            if (!senderAddress.isBlank()) {
                helper.setFrom(senderAddress, senderName);
            }
            helper.setText(buildMailContent(currentUser, content), false);
            for (MultipartFile image : images == null ? List.<MultipartFile>of() : images) {
                helper.addAttachment(safeFileName(image.getOriginalFilename()),
                        new ByteArrayResource(image.getBytes()), image.getContentType());
            }
            mailSender.send(message);
        } catch (MailException | MessagingException | java.io.IOException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "反馈发送失败，请稍后重试");
        }
    }

    private void validate(User currentUser, String content, List<MultipartFile> images) {
        if (currentUser == null || currentUser.getId() == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        if (content == null || content.isBlank()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "反馈内容不能为空");
        }
        if (content.length() > MAX_CONTENT_LENGTH) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "反馈内容不能超过1000个字符");
        }
        List<MultipartFile> safeImages = images == null ? List.of() : images;
        if (safeImages.size() > MAX_IMAGE_COUNT) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "最多上传3张图片");
        }
        for (MultipartFile image : safeImages) {
            if (image == null || image.isEmpty() || image.getSize() > MAX_IMAGE_SIZE) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "图片不能为空且不能超过5MB");
            }
            if (!ALLOWED_EXTENSIONS.contains(extensionOf(image.getOriginalFilename()))) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "仅支持 JPG、JPEG、PNG、WEBP 图片");
            }
        }
    }

    private String buildMailContent(User user, String content) {
        return "用户昵称：" + user.getUserName() + "\n"
                + "用户账号：" + user.getUserAccount() + "\n"
                + "用户 ID：" + user.getId() + "\n"
                + "提交时间：" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "\n\n"
                + "反馈内容：\n" + content;
    }

    private String extensionOf(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }

    private String safeFileName(String fileName) {
        return fileName == null ? "feedback-image" : fileName.replaceAll("[\\\\/:*?\"<>|]", "_");
    }
}
