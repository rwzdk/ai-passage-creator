package com.qc.template.controller;

import cn.hutool.core.bean.BeanUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.paginate.Page;
import com.qc.template.annotation.AuthCheck;
import com.qc.template.common.BaseResponse;
import com.qc.template.common.BatchDeleteRequest;
import com.qc.template.common.DeleteRequest;
import com.qc.template.common.ResultUtils;
import com.qc.template.constant.UserConstant;
import com.qc.template.exception.BusinessException;
import com.qc.template.exception.ErrorCode;
import com.qc.template.exception.ThrowUtils;
import com.qc.template.model.dto.user.*;
import com.qc.template.model.vo.LoginUserVO;
import com.qc.template.model.vo.UserVO;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import com.qc.template.model.entity.User;
import com.qc.template.service.CosService;
import com.qc.template.service.UserService;
import com.qc.template.service.RegistrationEmailVerificationService;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.io.IOException;

/**
 * 用户接口
 *
 */
@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private UserService userService;

    @Resource
    private RegistrationEmailVerificationService registrationEmailVerificationService;

    @Resource
    private CosService cosService;

    /**
     * 用户注册
     *
     * @param userRegisterRequest 注册请求
     * @return 注册结果
     */
    @PostMapping("/register")
    public BaseResponse<Long> userRegister(@RequestBody UserRegisterRequest userRegisterRequest) {
        ThrowUtils.throwIf(userRegisterRequest == null, ErrorCode.PARAMS_ERROR);
        String userAccount = userRegisterRequest.getUserAccount();
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();
        long result = userService.userRegister(userRegisterRequest);
        return ResultUtils.success(result);
    }

    @PostMapping("/register/email-code")
    public BaseResponse<Boolean> sendRegistrationEmailCode(
            @RequestBody RegistrationEmailCodeRequest registrationEmailCodeRequest) {
        ThrowUtils.throwIf(registrationEmailCodeRequest == null, ErrorCode.PARAMS_ERROR);
        registrationEmailVerificationService.sendVerificationCode(registrationEmailCodeRequest.getUserEmail());
        return ResultUtils.success(true);
    }

    /** 更新当前登录用户资料 */
    @PostMapping("/profile/update")
    public BaseResponse<LoginUserVO> updateProfile(
            @RequestBody UserProfileUpdateRequest profileUpdateRequest,
            HttpServletRequest request) {
        ThrowUtils.throwIf(profileUpdateRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(userService.updateCurrentProfile(profileUpdateRequest, loginUser));
    }

    /** 上传用户头像 */
    @PostMapping(value = "/avatar/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public BaseResponse<LoginUserVO> uploadAvatar(
            @RequestParam("file") MultipartFile file,
            HttpServletRequest request) throws IOException {
        ThrowUtils.throwIf(file == null || file.isEmpty(), ErrorCode.PARAMS_ERROR, "请选择头像文件");
        ThrowUtils.throwIf(file.getSize() > 5 * 1024 * 1024L, ErrorCode.PARAMS_ERROR, "头像文件不能超过 5MB");
        String contentType = file.getContentType();
        ThrowUtils.throwIf(contentType == null || !contentType.startsWith("image/"),
                ErrorCode.PARAMS_ERROR, "头像必须是图片文件");

        User loginUser = userService.getLoginUser(request);
        String avatarUrl = cosService.uploadBytes(file.getBytes(), contentType, "avatars");
        ThrowUtils.throwIf(avatarUrl == null || avatarUrl.isBlank(), ErrorCode.OPERATION_ERROR, "头像上传失败，请检查 COS 配置");

        User updateUser = new User();
        updateUser.setId(loginUser.getId());
        updateUser.setUserAvatar(avatarUrl);
        ThrowUtils.throwIf(!userService.updateById(updateUser), ErrorCode.OPERATION_ERROR, "头像保存失败");
        return ResultUtils.success(userService.getLoginUserVO(userService.getById(loginUser.getId())));
    }

    /**
     * 用户登录
     *
     * @param userLoginRequest 登录请求
     * @param request          请求对象
     * @return 登录结果
     */
    @PostMapping("/login")
    public BaseResponse<LoginUserVO> userLogin(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(userLoginRequest == null, ErrorCode.PARAMS_ERROR);
        String userAccount = userLoginRequest.getUserAccount();
        String userPassword = userLoginRequest.getUserPassword();
        LoginUserVO loginUserVO = userService.userLogin(userAccount, userPassword, request);
        return ResultUtils.success(loginUserVO);
    }

    @GetMapping("/get/login")
    public BaseResponse<LoginUserVO> getLoginUser(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(userService.getLoginUserVO(loginUser));
    }

    /**
     * 閻劍鍩涘▔銊╂敘
     *
     * @param request 请求对象
     * @return
     */
    @PostMapping("/logout")
    public BaseResponse<Boolean> userLogout(HttpServletRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        boolean result = userService.userLogout(request);
        return ResultUtils.success(result);
    }

    /**
     * 创建用户
     */
    @PostMapping("/add")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Long> addUser(@RequestBody UserAddRequest userAddRequest) {
        ThrowUtils.throwIf(userAddRequest == null, ErrorCode.PARAMS_ERROR);
        User user = new User();
        BeanUtil.copyProperties(userAddRequest, user);
        // 默认密码 12345678
        final String DEFAULT_PASSWORD = "12345678";
        String encryptPassword = userService.getEncryptPassword(DEFAULT_PASSWORD);
        user.setUserPassword(encryptPassword);
        boolean result = userService.save(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(user.getId());
    }

    /**
     * 根据 id 获取用户（仅管理员）
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<User> getUserById(long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        User user = userService.getById(id);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(user);
    }

    /**
     * 根据 id 获取用户视图对象
     */
    @GetMapping("/get/vo")
    public BaseResponse<UserVO> getUserVOById(long id) {
        BaseResponse<User> response = getUserById(id);
        User user = response.getData();
        return ResultUtils.success(userService.getUserVO(user));
    }

    /**
     * 删除用户
     */
    @PostMapping("/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteUser(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean b = userService.removeById(deleteRequest.getId());
        return ResultUtils.success(b);
    }

    /**
     * 批量删除用户
     */
    @PostMapping("/batch-delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Integer> batchDeleteUsers(@RequestBody BatchDeleteRequest batchDeleteRequest,
                                                   HttpServletRequest request) {
        ThrowUtils.throwIf(batchDeleteRequest == null || batchDeleteRequest.getIds() == null
                || batchDeleteRequest.getIds().isEmpty() || batchDeleteRequest.getIds().stream().anyMatch(id -> id == null || id <= 0),
                ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        ThrowUtils.throwIf(batchDeleteRequest.getIds().contains(loginUser.getId()),
                ErrorCode.PARAMS_ERROR, "不能删除当前登录的管理员账号");
        int deletedCount = 0;
        for (Long id : batchDeleteRequest.getIds()) {
            if (userService.removeById(id)) {
                deletedCount++;
            }
        }
        return ResultUtils.success(deletedCount);
    }

    /**
     * 更新用户
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateUser(@RequestBody UserUpdateRequest userUpdateRequest) {
        if (userUpdateRequest == null || userUpdateRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = new User();
        BeanUtil.copyProperties(userUpdateRequest, user);
        boolean result = userService.updateById(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 分页获取用户封装列表（仅管理员）
     *
     * @param userQueryRequest 查询请求参数
     */
    @PostMapping("/list/page/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<UserVO>> listUserVOByPage(@RequestBody UserQueryRequest userQueryRequest) {
        ThrowUtils.throwIf(userQueryRequest == null, ErrorCode.PARAMS_ERROR);
        long pageNum = userQueryRequest.getPageNum();
        long pageSize = userQueryRequest.getPageSize();
        Page<User> userPage = userService.page(Page.of(pageNum, pageSize),
                userService.getQueryWrapper(userQueryRequest));
        // 数据脱敏
        Page<UserVO> userVOPage = new Page<>(pageNum, pageSize, userPage.getTotalRow());
        List<UserVO> userVOList = userService.getUserVOList(userPage.getRecords());
        userVOPage.setRecords(userVOList);
        return ResultUtils.success(userVOPage);
    }

    /** 获取普通用户配额明细（仅管理员） */
    @GetMapping("/quota/list")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<List<UserVO>> listNormalUserQuotas() {
        List<User> normalUsers = userService.list(QueryWrapper.create()
                .eq("userRole", UserConstant.DEFAULT_ROLE)
                .orderBy("userAccount", true));
        return ResultUtils.success(userService.getUserVOList(normalUsers));
    }
}
