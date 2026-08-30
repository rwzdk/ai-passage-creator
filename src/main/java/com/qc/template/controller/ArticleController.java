package com.qc.template.controller;

import com.mybatisflex.core.paginate.Page;
import com.qc.template.common.BaseResponse;
import com.qc.template.common.BatchDeleteRequest;
import com.qc.template.common.DeleteRequest;
import com.qc.template.common.ResultUtils;
import com.qc.template.exception.ErrorCode;
import com.qc.template.exception.BusinessException;
import com.qc.template.exception.ThrowUtils;
import com.qc.template.manager.SseEmitterManager;
import com.qc.template.model.dto.article.ArticleAiModifyOutlineRequest;
import com.qc.template.model.dto.article.ArticleConfirmOutlineRequest;
import com.qc.template.model.dto.article.ArticleConfirmTitleRequest;
import com.qc.template.model.dto.article.ArticleCreateRequest;
import com.qc.template.model.dto.article.ArticleQueryRequest;
import com.qc.template.model.dto.article.ArticleAiEditRequest;
import com.qc.template.model.dto.article.ArticleRegenerateImageRequest;
import com.qc.template.model.dto.article.ArticleSelectImageVersionRequest;
import com.qc.template.model.dto.article.ArticleState;
import com.qc.template.model.dto.article.ArticleUpdateContentRequest;
import com.qc.template.model.entity.Article;

import java.util.List;
import java.util.Map;
import com.qc.template.model.entity.User;
import com.qc.template.model.enums.ArticlePhaseEnum;
import com.qc.template.model.enums.ArticleStatusEnum;
import com.qc.template.model.enums.ArticleStyleEnum;
import com.qc.template.model.vo.AgentExecutionStats;
import com.qc.template.model.vo.ArticleVO;
import com.qc.template.model.vo.DocumentReferenceVO;
import com.qc.template.model.vo.UserArticleStatsVO;
import com.qc.template.model.vo.HotTopicsVO;
import com.qc.template.service.AgentLogService;
import com.qc.template.service.ArticleAsyncService;
import com.qc.template.service.ArticleService;
import com.qc.template.service.DocumentReferenceService;
import com.qc.template.service.UserService;
import com.qc.template.service.HotTopicService;
import com.qc.template.utils.GsonUtils;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 文章接口
 */
@RestController
@RequestMapping("/article")
@Slf4j
public class ArticleController {

    @Resource
    private ArticleService articleService;

    @Resource
    private ArticleAsyncService articleAsyncService;

    @Resource
    private SseEmitterManager sseEmitterManager;

    @Resource
    private UserService userService;

    @Resource
    private AgentLogService agentLogService;

    @Resource
    private HotTopicService hotTopicService;

    @Resource
    private DocumentReferenceService documentReferenceService;

    /** 上传并总结本次创作使用的参考文档，不保存原文件。 */
    @PostMapping(value = "/reference/parse", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "解析文章参考文档")
    public BaseResponse<DocumentReferenceVO> parseReferenceDocument(
            @RequestParam("file") MultipartFile file,
            HttpServletRequest httpServletRequest) {
        userService.getLoginUser(httpServletRequest);
        try {
            return ResultUtils.success(documentReferenceService.parse(file));
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, e.getMessage());
        }
    }

    @GetMapping("/hot-topics")
    @Operation(summary = "获取热门选题")
    public BaseResponse<HotTopicsVO> getHotTopics(
            @RequestParam(value = "refresh", defaultValue = "false") boolean refresh) {
        return ResultUtils.success(hotTopicService.getHotTopics(refresh));
    }

    /**
     * 创建文章任务
     */
    @PostMapping("/create")
    @Operation(summary = "创建文章任务")
    public BaseResponse<String> createArticle(@RequestBody ArticleCreateRequest request, HttpServletRequest httpServletRequest) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(request.getTopic() == null || request.getTopic().trim().isEmpty(), 
                ErrorCode.PARAMS_ERROR, "选题不能为空");
        // 校验风格参数（允许为空）
        ThrowUtils.throwIf(!ArticleStyleEnum.isValid(request.getStyle()),
                ErrorCode.PARAMS_ERROR, "无效的文章风格");

        User loginUser = userService.getLoginUser(httpServletRequest);

        // 检查并消耗配额，创建文章任务（在同一事务中）
        String taskId = articleService.createArticleTaskWithQuotaCheck(
                request.getTopic(), 
                request.getStyle(), 
                request.getEnabledImageMethods(),
                request.getReferenceSummary(),
                loginUser
        );

        return ResultUtils.success(taskId);
    }

    /**
     * 在 SSE 连接建立后启动阶段1，避免标题流式事件早于前端连接而丢失。
     */
    @PostMapping("/start/{taskId}")
    @Operation(summary = "启动文章标题生成")
    public BaseResponse<Boolean> startArticle(@PathVariable String taskId, HttpServletRequest httpServletRequest) {
        ThrowUtils.throwIf(taskId == null || taskId.trim().isEmpty(),
                ErrorCode.PARAMS_ERROR, "任务ID不能为空");

        User loginUser = userService.getLoginUser(httpServletRequest);
        articleService.getArticleDetail(taskId, loginUser);
        Article article = articleService.getByTaskId(taskId);
        if (article != null && ArticlePhaseEnum.PENDING.getValue().equals(article.getPhase())) {
            articleAsyncService.executePhase1(
                    taskId,
                    article.getTopic(),
                    article.getStyle(),
                    article.getReferenceSummary()
            );
        }
        return ResultUtils.success(true);
    }

    /**
     * SSE 进度推送
     */
    @GetMapping("/progress/{taskId}")
    @Operation(summary = "获取文章生成进度(SSE)")
    public SseEmitter getProgress(@PathVariable String taskId, HttpServletRequest httpServletRequest) {
        ThrowUtils.throwIf(taskId == null || taskId.trim().isEmpty(), 
                ErrorCode.PARAMS_ERROR, "任务ID不能为空");

        // 校验权限（内部会检查任务是否存在以及用户是否有权限访问）
        User loginUser = userService.getLoginUser(httpServletRequest);
        articleService.getArticleDetail(taskId, loginUser);

        // 创建 SSE Emitter
        SseEmitter emitter = sseEmitterManager.createEmitter(taskId);

        // 先发送握手事件，确保浏览器收到响应并触发 EventSource.onopen，随后前端再启动阶段1。
        sseEmitterManager.send(taskId, GsonUtils.toJson(Map.of("type", "SSE_READY")));

        // EventSource 重连后若任务已结束，立即补发终态供前端同步最终图文。
        if (ArticleStatusEnum.COMPLETED.getValue().equals(articleService.getArticleDetail(taskId, loginUser).getStatus())) {
            sseEmitterManager.send(taskId, GsonUtils.toJson(Map.of("type", "ALL_COMPLETE")));
        }
        
        log.info("SSE 连接已建立, taskId={}", taskId);
        return emitter;
    }

    /**
     * 获取文章详情
     */
    @GetMapping("/{taskId}")
    @Operation(summary = "获取文章详情")
    public BaseResponse<ArticleVO> getArticle(@PathVariable String taskId, HttpServletRequest httpServletRequest) {
        ThrowUtils.throwIf(taskId == null || taskId.trim().isEmpty(), 
                ErrorCode.PARAMS_ERROR, "任务ID不能为空");

        User loginUser = userService.getLoginUser(httpServletRequest);
        ArticleVO articleVO = articleService.getArticleDetail(taskId, loginUser);

        return ResultUtils.success(articleVO);
    }

    @PostMapping("/select-image-version")
    @Operation(summary = "切换文章配图版本")
    public BaseResponse<ArticleVO> selectArticleImageVersion(@RequestBody ArticleSelectImageVersionRequest request,
                                                              HttpServletRequest httpServletRequest) {
        ThrowUtils.throwIf(request == null || request.getTaskId() == null || request.getTaskId().isBlank(),
                ErrorCode.PARAMS_ERROR, "任务ID不能为空");
        ThrowUtils.throwIf(request.getPosition() == null || request.getPosition() < 1,
                ErrorCode.PARAMS_ERROR, "图片位置不合法");
        ThrowUtils.throwIf(request.getVersionId() == null || request.getVersionId().isBlank(),
                ErrorCode.PARAMS_ERROR, "图片版本不能为空");
        return ResultUtils.success(articleService.selectArticleImageVersion(request.getTaskId(), request.getPosition(),
                request.getVersionId(), userService.getLoginUser(httpServletRequest)));
    }

    @PostMapping("/update-content")
    @Operation(summary = "保存文章编辑内容")
    public BaseResponse<Boolean> updateArticleContent(@RequestBody ArticleUpdateContentRequest request,
                                                      HttpServletRequest httpServletRequest) {
        ThrowUtils.throwIf(request == null || request.getTaskId() == null || request.getTaskId().isBlank(),
                ErrorCode.PARAMS_ERROR, "任务ID不能为空");
        ThrowUtils.throwIf(request.getContent() == null || request.getContent().isBlank(),
                ErrorCode.PARAMS_ERROR, "文章内容不能为空");
        articleService.updateArticleContent(request.getTaskId(), request.getMainTitle(), request.getSubTitle(), request.getContent(),
                userService.getLoginUser(httpServletRequest));
        return ResultUtils.success(true);
    }

    @PostMapping("/ai-edit-content")
    @Operation(summary = "AI 编辑文章内容")
    public BaseResponse<String> aiEditArticleContent(@RequestBody ArticleAiEditRequest request,
                                                     HttpServletRequest httpServletRequest) {
        ThrowUtils.throwIf(request == null || request.getTaskId() == null || request.getTaskId().isBlank(),
                ErrorCode.PARAMS_ERROR, "任务ID不能为空");
        ThrowUtils.throwIf(request.getContent() == null || request.getContent().isBlank(),
                ErrorCode.PARAMS_ERROR, "文章内容不能为空");
        ThrowUtils.throwIf(request.getInstruction() == null || request.getInstruction().isBlank(),
                ErrorCode.PARAMS_ERROR, "请输入 AI 修改要求");
        String content = articleService.aiEditArticleContent(request.getTaskId(), request.getContent(),
                request.getInstruction(), userService.getLoginUser(httpServletRequest));
        return ResultUtils.success(content);
    }

    @PostMapping("/regenerate-image")
    @Operation(summary = "重新生成文章配图")
    public BaseResponse<ArticleVO> regenerateArticleImage(@RequestBody ArticleRegenerateImageRequest request,
                                                          HttpServletRequest httpServletRequest) {
        ThrowUtils.throwIf(request == null || request.getTaskId() == null || request.getTaskId().isBlank(),
                ErrorCode.PARAMS_ERROR, "任务ID不能为空");
        ThrowUtils.throwIf(request.getPosition() == null || request.getPosition() < 1,
                ErrorCode.PARAMS_ERROR, "图片位置不合法");
        ThrowUtils.throwIf(request.getPrompt() == null || request.getPrompt().isBlank(),
                ErrorCode.PARAMS_ERROR, "请输入图片修改说明");
        ArticleVO articleVO = articleService.regenerateArticleImage(request.getTaskId(), request.getPosition(),
                request.getPrompt(), userService.getLoginUser(httpServletRequest));
        return ResultUtils.success(articleVO);
    }

    /**
     * 分页查询文章列表
     */
    @PostMapping("/list")
    @Operation(summary = "分页查询文章列表")
    public BaseResponse<Page<ArticleVO>> listArticle(@RequestBody ArticleQueryRequest request,
                                                       HttpServletRequest httpServletRequest) {
        User loginUser = userService.getLoginUser(httpServletRequest);
        Page<ArticleVO> articleVOPage = articleService.listArticleByPage(request, loginUser);
        
        return ResultUtils.success(articleVOPage);
    }

    /** 获取个人创作统计 */
    @GetMapping("/profile/stats")
    @Operation(summary = "获取个人创作统计")
    public BaseResponse<UserArticleStatsVO> getUserArticleStats(HttpServletRequest httpServletRequest) {
        User loginUser = userService.getLoginUser(httpServletRequest);
        return ResultUtils.success(articleService.getUserArticleStats(loginUser));
    }

    /**
     * 删除文章
     */
    @PostMapping("/delete")
    @Operation(summary = "删除文章")
    public BaseResponse<Boolean> deleteArticle(@RequestBody DeleteRequest deleteRequest,
                                                 HttpServletRequest httpServletRequest) {
        ThrowUtils.throwIf(deleteRequest == null || deleteRequest.getId() == null, 
                ErrorCode.PARAMS_ERROR);
        
        User loginUser = userService.getLoginUser(httpServletRequest);
        boolean result = articleService.deleteArticle(deleteRequest.getId(), loginUser);
        
        return ResultUtils.success(result);
    }

    /**
     * 批量删除文章
     */
    @PostMapping("/batch-delete")
    @Operation(summary = "批量删除文章")
    public BaseResponse<Integer> batchDeleteArticles(@RequestBody BatchDeleteRequest batchDeleteRequest,
                                                      HttpServletRequest httpServletRequest) {
        ThrowUtils.throwIf(batchDeleteRequest == null || batchDeleteRequest.getIds() == null
                || batchDeleteRequest.getIds().isEmpty() || batchDeleteRequest.getIds().stream().anyMatch(id -> id == null || id <= 0),
                ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(httpServletRequest);
        return ResultUtils.success(articleService.deleteArticles(batchDeleteRequest.getIds(), loginUser));
    }

    /**
     * 确认标题并输入补充描述
     */
    @PostMapping("/confirm-title")
    @Operation(summary = "确认标题并输入补充描述")
    public BaseResponse<Void> confirmTitle(@RequestBody ArticleConfirmTitleRequest request,
                                            HttpServletRequest httpServletRequest) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(request.getTaskId() == null || request.getTaskId().trim().isEmpty(),
                ErrorCode.PARAMS_ERROR, "任务ID不能为空");
        ThrowUtils.throwIf(request.getSelectedMainTitle() == null || request.getSelectedMainTitle().trim().isEmpty(),
                ErrorCode.PARAMS_ERROR, "主标题不能为空");
        ThrowUtils.throwIf(request.getSelectedSubTitle() == null || request.getSelectedSubTitle().trim().isEmpty(),
                ErrorCode.PARAMS_ERROR, "副标题不能为空");

        User loginUser = userService.getLoginUser(httpServletRequest);

        // 确认标题
        articleService.confirmTitle(
                request.getTaskId(),
                request.getSelectedMainTitle(),
                request.getSelectedSubTitle(),
                request.getUserDescription(),
                loginUser
        );

        // 异步执行阶段2：生成文章大纲
        articleAsyncService.executePhase2(request.getTaskId());

        return ResultUtils.success(null);
    }

    /**
     * 确认大纲
     */
    @PostMapping("/confirm-outline")
    @Operation(summary = "确认大纲")
    public BaseResponse<Void> confirmOutline(@RequestBody ArticleConfirmOutlineRequest request,
                                              HttpServletRequest httpServletRequest) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(request.getTaskId() == null || request.getTaskId().trim().isEmpty(),
                ErrorCode.PARAMS_ERROR, "任务ID不能为空");
        ThrowUtils.throwIf(request.getOutline() == null || request.getOutline().isEmpty(),
                ErrorCode.PARAMS_ERROR, "大纲不能为空");

        User loginUser = userService.getLoginUser(httpServletRequest);

        // 确认大纲
        articleService.confirmOutline(
                request.getTaskId(),
                request.getOutline(),
                loginUser
        );

        // 异步执行阶段3：生成正文、分析配图并合成图文
        articleAsyncService.executePhase3(request.getTaskId());

        return ResultUtils.success(null);
    }

    /**
     * AI 修改大纲
     */
    @PostMapping("/ai-modify-outline")
    @Operation(summary = "AI 修改大纲")
    public BaseResponse<List<ArticleState.OutlineSection>> aiModifyOutline(
            @RequestBody ArticleAiModifyOutlineRequest request,
            HttpServletRequest httpServletRequest) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(request.getTaskId() == null || request.getTaskId().trim().isEmpty(),
                ErrorCode.PARAMS_ERROR, "任务ID不能为空");
        ThrowUtils.throwIf(request.getModifySuggestion() == null || request.getModifySuggestion().trim().isEmpty(),
                ErrorCode.PARAMS_ERROR, "修改建议不能为空");

        User loginUser = userService.getLoginUser(httpServletRequest);

        // AI 修改大纲
        List<ArticleState.OutlineSection> modifiedOutline = articleService.aiModifyOutline(
                request.getTaskId(),
                request.getModifySuggestion(),
                loginUser
        );

        return ResultUtils.success(modifiedOutline);
    }

    /**
     * 获取智能体执行日志
     */
    @GetMapping("/execution-logs/{taskId}")
    @Operation(summary = "获取智能体执行日志")
    public BaseResponse<AgentExecutionStats> getExecutionLogs(@PathVariable String taskId) {
        ThrowUtils.throwIf(taskId == null || taskId.trim().isEmpty(), 
                ErrorCode.PARAMS_ERROR, "任务ID不能为空");
        
        AgentExecutionStats stats = agentLogService.getExecutionStats(taskId);
        return ResultUtils.success(stats);
    }
}
