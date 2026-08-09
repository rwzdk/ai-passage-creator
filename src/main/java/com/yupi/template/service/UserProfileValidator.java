package com.yupi.template.service;

import com.yupi.template.model.dto.user.UserProfileUpdateRequest;

import java.util.regex.Pattern;

/** 用户资料字段的共享校验规则。 */
public final class UserProfileValidator {

    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final Pattern PHONE = Pattern.compile("^[0-9+()\\-\\s]{6,20}$");
    private static final Pattern URL = Pattern.compile("^https?://[^\\s]+$");

    private UserProfileValidator() {
    }

    public static void validate(UserProfileUpdateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("资料不能为空");
        }
        checkLength(request.getUserName(), 50, "昵称");
        checkLength(request.getUserProfile(), 500, "个人简介");
        checkLength(request.getUserEmail(), 128, "邮箱");
        checkLength(request.getUserPhone(), 32, "手机号");
        checkLength(request.getUserBlog(), 255, "博客地址");
        checkLength(request.getUserGithub(), 255, "GitHub 地址");
        checkPattern(request.getUserEmail(), EMAIL, "邮箱格式不正确");
        checkPattern(request.getUserPhone(), PHONE, "手机号格式不正确");
        checkPattern(request.getUserBlog(), URL, "博客地址必须以 http:// 或 https:// 开头");
        checkPattern(request.getUserGithub(), URL, "GitHub 地址必须以 http:// 或 https:// 开头");
    }

    private static void checkLength(String value, int maxLength, String fieldName) {
        if (value != null && value.length() > maxLength) {
            throw new IllegalArgumentException(fieldName + "长度不能超过" + maxLength + "个字符");
        }
    }

    private static void checkPattern(String value, Pattern pattern, String message) {
        if (value != null && !value.isBlank() && !pattern.matcher(value).matches()) {
            throw new IllegalArgumentException(message);
        }
    }
}
