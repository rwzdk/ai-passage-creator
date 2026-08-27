package com.qc.template.controller;

import com.qc.template.common.BaseResponse;
import com.qc.template.common.ResultUtils;
import com.qc.template.model.entity.User;
import com.qc.template.service.FeedbackService;
import com.qc.template.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/feedback")
public class FeedbackController {

    @Resource
    private UserService userService;

    @Resource
    private FeedbackService feedbackService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "提交平台反馈建议")
    public BaseResponse<Boolean> submit(@RequestParam String content,
                                        @RequestParam(value = "images", required = false) List<MultipartFile> images,
                                        HttpServletRequest request) {
        User currentUser = userService.getLoginUser(request);
        feedbackService.sendFeedback(currentUser, content, images);
        return ResultUtils.success(true);
    }
}
