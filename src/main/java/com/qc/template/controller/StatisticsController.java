package com.qc.template.controller;

import com.qc.template.annotation.AuthCheck;
import com.qc.template.common.BaseResponse;
import com.qc.template.common.ResultUtils;
import com.qc.template.constant.UserConstant;
import com.qc.template.model.vo.StatisticsVO;
import com.qc.template.service.StatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 缁熻鍒嗘瀽鎺у埗鍣?
 *
 */
@RestController
@RequestMapping("/statistics")
@Slf4j
@Tag(name = "StatisticsController", description = "缁熻鍒嗘瀽鎺ュ彛")
public class StatisticsController {

    @Resource
    private StatisticsService statisticsService;

    /**
     * 鑾峰彇绯荤粺缁熻鏁版嵁锛堜粎绠＄悊鍛橈級
     */
    @GetMapping("/overview")
    @Operation(summary = "鑾峰彇绯荤粺缁熻鏁版嵁")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<StatisticsVO> getStatistics(
            @RequestParam(defaultValue = "false") boolean refresh) {
        StatisticsVO statistics = statisticsService.getStatistics(refresh);
        return ResultUtils.success(statistics);
    }
}
