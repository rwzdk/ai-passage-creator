package com.qc.template.service;

import com.qc.template.model.dto.user.UserProfileUpdateRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UserProfileValidatorTest {

    @Test
    void acceptsOptionalContactFieldsWhenTheyAreValid() {
        UserProfileUpdateRequest request = new UserProfileUpdateRequest();
        request.setUserName("诗人用户");
        request.setUserEmail("poet@qq.com");
        request.setUserPhone("13800138000");
        request.setUserBlog("https://example.com/blog");
        request.setUserGithub("https://github.com/example");

        assertDoesNotThrow(() -> UserProfileValidator.validate(request));
    }

    @Test
    void rejectsInvalidEmail() {
        UserProfileUpdateRequest request = new UserProfileUpdateRequest();
        request.setUserEmail("not-an-email");

        assertThrows(IllegalArgumentException.class, () -> UserProfileValidator.validate(request));
    }
}
