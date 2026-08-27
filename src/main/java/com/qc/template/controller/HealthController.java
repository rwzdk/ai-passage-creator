package com.qc.template.controller;

import com.qc.template.common.BaseResponse;
import com.qc.template.common.ResultUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 健康检查控制器
 * 用于 Docker 容器健康检查和负载均衡器探针
 */
@RestController
@RequestMapping("/health")
public class HealthController {

    /**
     * 健康检查端点（支持有无末尾斜杠）
     */
    @GetMapping({"", "/"})
    public BaseResponse<String> healthCheck() {
        return ResultUtils.success("ok");
    }
}
