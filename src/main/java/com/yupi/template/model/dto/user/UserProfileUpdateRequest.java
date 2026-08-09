package com.yupi.template.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/** 当前登录用户的资料更新请求。 */
@Data
public class UserProfileUpdateRequest implements Serializable {

    private String userName;
    private String userProfile;
    private String userEmail;
    private String userPhone;
    private String userBlog;
    private String userGithub;

    private static final long serialVersionUID = 1L;
}
