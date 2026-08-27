package com.qc.template.service;

import com.qc.template.exception.BusinessException;
import com.qc.template.model.entity.User;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FeedbackServiceTest {

    private final JavaMailSender mailSender = mock(JavaMailSender.class);
    private final FeedbackService feedbackService = new FeedbackService(
            mailSender, "235173498@qq.com", "YuanJian Studio", "sender@qq.com");

    @Test
    void rejectsBlankFeedbackBeforeSendingMail() {
        BusinessException exception = assertThrows(BusinessException.class,
                () -> feedbackService.sendFeedback(user(), "  ", List.of()));

        assertEquals("反馈内容不能为空", exception.getMessage());
    }

    @Test
    void rejectsUnsupportedFeedbackImage() {
        MockMultipartFile invalidImage = new MockMultipartFile(
                "images", "feedback.pdf", "application/pdf", new byte[]{1});

        BusinessException exception = assertThrows(BusinessException.class,
                () -> feedbackService.sendFeedback(user(), "附件格式测试", List.of(invalidImage)));

        assertEquals("仅支持 JPG、JPEG、PNG、WEBP 图片", exception.getMessage());
    }

    @Test
    void sendsUserFeedbackAndImageAttachment() throws Exception {
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        MockMultipartFile image = new MockMultipartFile(
                "images", "screen.png", "image/png", new byte[]{1, 2, 3});

        feedbackService.sendFeedback(user(), "希望增加更多导出格式", List.of(image));

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());
        assertTrue(captor.getValue().getSubject().contains("创作平台反馈建议"));
        assertEquals("YuanJian Studio", ((InternetAddress) captor.getValue().getFrom()[0]).getPersonal());
        MimeMultipart multipart = (MimeMultipart) captor.getValue().getContent();
        assertEquals(2, multipart.getCount());
        MimeMultipart related = (MimeMultipart) multipart.getBodyPart(0).getContent();
        assertTrue(related.getBodyPart(0).getContent().toString().contains("希望增加更多导出格式"));
    }

    private User user() {
        User user = new User();
        user.setId(1L);
        user.setUserName("测试用户");
        user.setUserAccount("test-user");
        return user;
    }
}
