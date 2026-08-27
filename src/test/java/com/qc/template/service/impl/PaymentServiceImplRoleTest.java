package com.qc.template.service.impl;

import com.qc.template.mapper.UserMapper;
import com.qc.template.model.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentServiceImplRoleTest {

    private PaymentServiceImpl paymentService;
    private UserMapper userMapper;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentServiceImpl();
        userMapper = Mockito.mock(UserMapper.class);
        ReflectionTestUtils.setField(paymentService, "userMapper", userMapper);
    }

    @Test
    void adminRemainsAdminAfterVipUpgrade() {
        when(userMapper.selectOneById(1L))
                .thenReturn(User.builder().id(1L).userRole("admin").build());

        ReflectionTestUtils.invokeMethod(paymentService, "upgradeUserToVip", 1L);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).update(captor.capture());

        assertEquals(null, captor.getValue().getUserRole());
    }

    @Test
    void adminCanPassVipValidation() {
        ReflectionTestUtils.invokeMethod(
                paymentService,
                "validateIsVip",
                User.builder().userRole("admin").build()
        );
    }

    @Test
    void adminRemainsAdminAfterVipRevoke() {
        User admin = User.builder().id(1L).userRole("admin").quota(5).build();
        when(userMapper.selectOneById(1L)).thenReturn(admin);

        ReflectionTestUtils.invokeMethod(paymentService, "revokeVipStatus", 1L);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).update(captor.capture());

        assertEquals(null, captor.getValue().getUserRole());
    }
}
