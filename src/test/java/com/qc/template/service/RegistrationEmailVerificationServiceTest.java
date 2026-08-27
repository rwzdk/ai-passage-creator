package com.qc.template.service;

import com.qc.template.exception.BusinessException;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RegistrationEmailVerificationServiceTest {

    private final JavaMailSender mailSender = mock(JavaMailSender.class);
    private final RegistrationEmailVerificationService verificationService =
            new RegistrationEmailVerificationService(mailSender, "YuanJian Studio", "sender@qq.com");

    @Test
    void sendsCodeToQqEmailAndConsumesItAfterSuccessfulVerification() throws Exception {
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        verificationService.sendVerificationCode("poet@qq.com");

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());
        assertEquals("poet@qq.com", captor.getValue().getAllRecipients()[0].toString());
        Matcher matcher = Pattern.compile("(?<!\\d)\\d{6}(?!\\d)").matcher(captor.getValue().getContent().toString());
        assertTrue(matcher.find());
        String code = matcher.group();
        assertDoesNotThrow(() -> verificationService.verifyAndConsume("poet@qq.com", code));
        assertThrows(BusinessException.class, () -> verificationService.verifyAndConsume("poet@qq.com", code));
    }

    @Test
    void rejectsNonQqEmailBeforeSendingCode() {
        BusinessException exception = assertThrows(BusinessException.class,
                () -> verificationService.sendVerificationCode("poet@example.com"));

        assertEquals("请输入有效的 QQ 邮箱", exception.getMessage());
    }
}
