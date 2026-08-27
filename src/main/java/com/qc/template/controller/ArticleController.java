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

import java.util.List;
import java.util.Map;
import com.qc.template.model.entity.User;
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
 * 鏂囩珷鎺ュ彛
 *
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

    /** 涓婁紶骞舵€荤粨鏈鍒涗綔浣跨敤鐨勫弬鑰冩枃妗ｏ紝涓嶄繚瀛樺師鏂囦欢銆?*/
    @PostMapping(value = "/reference/parse", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "瑙ｆ瀽鏂囩珷鍙傝€冩枃妗?)
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
    @Operation(summary = "鑾峰彇鐑棬閫夐")
    public BaseResponse<HotTopicsVO> getHotTopics(
            @RequestParam(value = "refresh", defaultValue = "false") boolean refresh) {
        return ResultUtils.success(hotTopicService.getHotTopics(refresh));
    }

    /**
     * 鍒涘缓鏂囩珷浠诲姟
     */
    @PostMapping("/create")
    @Operation(summary = "鍒涘缓鏂囩珷浠诲姟")
    public BaseResponse<String> createArticle(@RequestBody ArticleCreateRequest request, HttpServletRequest httpServletRequest) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(request.getTopic() == null || request.getTopic().trim().isEmpty(), 
                ErrorCode.PARAMS_ERROR, "閫夐涓嶈兘涓虹┖");
        // 鏍￠獙椋庢牸鍙傛暟锛堝厑璁镐负绌猴級
        ThrowUtils.throwIf(!ArticleStyleEnum.isValid(request.getStyle()),
                ErrorCode.PARAMS_ERROR, "鏃犳晥鐨勬枃绔犻鏍?);

        User loginUser = userService.getLoginUser(httpServletRequest);

        // 妫€鏌ュ苟娑堣€楅厤棰?+ 鍒涘缓鏂囩珷浠诲姟锛堝湪鍚屼竴浜嬪姟涓級
        String taskId = articleService.createArticleTaskWithQuotaCheck(
                request.getTopic(), 
                request.getStyle(), 
                request.getEnabledImageMethods(),
                request.getReferenceSummary(),
                loginUser
        );

        // 寮傛鎵ц闃舵1锛氱敓鎴愭爣棰樻柟妗?
        articleAsyncService.executePhase1(
                taskId, 
                request.getTopic(),
                request.getStyle(),
                request.getReferenceSummary()
        );

        return ResultUtils.success(taskId);
    }

    /**
     * SSE 杩涘害鎺ㄩ€?
     */
    @GetMapping("/progress/{taskId}")
    @Operation(summary = "鑾峰彇鏂囩珷鐢熸垚杩涘害(SSE)")
    public SseEmitter getProgress(@PathVariable String taskId, HttpServletRequest httpServletRequest) {
        ThrowUtils.throwIf(taskId == null || taskId.trim().isEmpty(), 
                ErrorCode.PARAMS_ERROR, "浠诲姟ID涓嶈兘涓虹┖");

        // 鏍￠獙鏉冮檺锛堝唴閮ㄤ細妫€鏌ヤ换鍔℃槸鍚﹀瓨鍦ㄤ互鍙婄敤鎴锋槸鍚︽湁鏉冮檺璁块棶锛?
        User loginUser = userService.getLoginUser(httpServletRequest);
        articleService.getArticleDetail(taskId, loginUser);

        // 鍒涘缓 SSE Emitter
        SseEmitter emitter = sseEmitterManager.createEmitter(taskId);

        // EventSource 閲嶈繛鍚庤嫢浠诲姟宸茬粨鏉燂紝绔嬪嵆琛ュ彂缁堟€佷緵鍓嶇鍚屾鏈€缁堝浘鏂囥€?        if (ArticleStatusEnum.COMPLETED.getValue().equals(articleService.getArticleDetail(taskId, loginUser).getStatus())) {
            sseEmitterManager.send(taskId, GsonUtils.toJson(Map.of("type", "ALL_COMPLETE")));
        }
        
        log.info("SSE 杩炴帴宸插缓绔? taskId={}", taskId);
        return emitter;
    }

    /**
     * 鑾峰彇鏂囩珷璇︽儏
     */
    @GetMapping("/{taskId}")
    @Operation(summary = "鑾峰彇鏂囩珷璇︽儏")
    public BaseResponse<ArticleVO> getArticle(@PathVariable String taskId, HttpServletRequest httpServletRequest) {
        ThrowUtils.throwIf(taskId == null || taskId.trim().isEmpty(), 
                ErrorCode.PARAMS_ERROR, "浠诲姟ID涓嶈兘涓虹┖");

        User loginUser = userService.getLoginUser(httpServletRequest);
        ArticleVO articleVO = articleService.getArticleDetail(taskId, loginUser);

        return ResultUtils.success(articleVO);
    }

    @PostMapping("/select-image-version")
    @Operation(summary = "鍒囨崲鏂囩珷閰嶅浘鐗堟湰")
    public BaseResponse<ArticleVO> selectArticleImageVersion(@RequestBody ArticleSelectImageVersionRequest request,
                                                              HttpServletRequest httpServletRequest) {
        ThrowUtils.throwIf(request == null || request.getTaskId() == null || request.getTaskId().isBlank(),
                ErrorCode.PARAMS_ERROR, "浠诲姟ID涓嶈兘涓虹┖");
        ThrowUtils.throwIf(request.getPosition() == null || request.getPosition() < 1,
                ErrorCode.PARAMS_ERROR, "鍥剧墖浣嶇疆涓嶅悎娉?);
        ThrowUtils.throwIf(request.getVersionId() == null || request.getVersionId().isBlank(),
                ErrorCode.PARAMS_ERROR, "鍥剧墖鐗堟湰涓嶈兘涓虹┖");
        return ResultUtils.success(articleService.selectArticleImageVersion(request.getTaskId(), request.getPosition(),
                request.getVersionId(), userService.getLoginUser(httpServletRequest)));
    }

    @PostMapping("/update-content")
    @Operation(summary = "淇濆瓨鏂囩珷缂栬緫鍐呭")
    public BaseResponse<Boolean> updateArticleContent(@RequestBody ArticleUpdateContentRequest request,
                                                      HttpServletRequest httpServletRequest) {
        ThrowUtils.throwIf(request == null || request.getTaskId() == null || request.getTaskId().isBlank(),
                ErrorCode.PARAMS_ERROR, "浠诲姟ID涓嶈兘涓虹┖");
        ThrowUtils.throwIf(request.getContent() == null || request.getContent().isBlank(),
                ErrorCode.PARAMS_ERROR, "鏂囩珷鍐呭涓嶈兘涓虹┖");
        articleService.updateArticleContent(request.getTaskId(), request.getMainTitle(), request.getSubTitle(), request.getContent(),
                userService.getLoginUser(httpServletRequest));
        return ResultUtils.success(true);
    }

    @PostMapping("/ai-edit-content")
    @Operation(summary = "AI 缂栬緫鏂囩珷鍐呭")
    public BaseResponse<String> aiEditArticleContent(@RequestBody ArticleAiEditRequest request,
                                                     HttpServletRequest httpServletRequest) {
        ThrowUtils.throwIf(request == null || request.getTaskId() == null || request.getTaskId().isBlank(),
                ErrorCode.PARAMS_ERROR, "浠诲姟ID涓嶈兘涓虹┖");
        ThrowUtils.throwIf(request.getContent() == null || request.getContent().isBlank(),
                ErrorCode.PARAMS_ERROR, "鏂囩珷鍐呭涓嶈兘涓虹┖");
        ThrowUtils.throwIf(request.getInstruction() == null || request.getInstruction().isBlank(),
                ErrorCode.PARAMS_ERROR, "璇疯緭鍏?AI 淇敼瑕佹眰");
        String content = articleService.aiEditArticleContent(request.getTaskId(), request.getContent(),
                request.getInstruction(), userService.getLoginUser(httpServletRequest));
        return ResultUtils.success(content);
    }

    @PostMapping("/regenerate-image")
    @Operation(summary = "閲嶆柊鐢熸垚鏂囩珷閰嶅浘")
    public BaseResponse<ArticleVO> regenerateArticleImage(@RequestBody ArticleRegenerateImageRequest request,
                                                          HttpServletRequest httpServletRequest) {
        ThrowUtils.throwIf(request == null || request.getTaskId() == null || request.getTaskId().isBlank(),
                ErrorCode.PARAMS_ERROR, "浠诲姟ID涓嶈兘涓虹┖");
        ThrowUtils.throwIf(request.getPosition() == null || request.getPosition() < 1,
                ErrorCode.PARAMS_ERROR, "鍥剧墖浣嶇疆涓嶅悎娉?);
        ThrowUtils.throwIf(request.getPrompt() == null || request.getPrompt().isBlank(),
                ErrorCode.PARAMS_ERROR, "璇疯緭鍏ュ浘鐗囦慨鏀硅鏄?);
        ArticleVO articleVO = articleService.regenerateArticleImage(request.getTaskId(), request.getPosition(),
                request.getPrompt(), userService.getLoginUser(httpServletRequest));
        return ResultUtils.success(articleVO);
    }

    /**
     * 鍒嗛〉鏌ヨ鏂囩珷鍒楄〃
     */
    @PostMapping("/list")
    @Operation(summary = "鍒嗛〉鏌ヨ鏂囩珷鍒楄〃")
    public BaseResponse<Page<ArticleVO>> listArticle(@RequestBody ArticleQueryRequest request,
                                                       HttpServletRequest httpServletRequest) {
        User loginUser = userService.getLoginUser(httpServletRequest);
        Page<ArticleVO> articleVOPage = articleService.listArticleByPage(request, loginUser);
        
        return ResultUtils.success(articleVOPage);
    }

    /** 鑾峰彇褰撳墠鐢ㄦ埛鐨勫垱浣滅粺璁°€?*/
    @GetMapping("/profile/stats")
    @Operation(summary = "鑾峰彇涓汉鍒涗綔缁熻")
    public BaseResponse<UserArticleStatsVO> getUserArticleStats(HttpServletRequest httpServletRequest) {
        User loginUser = userService.getLoginUser(httpServletRequest);
        return ResultUtils.success(articleService.getUserArticleStats(loginUser));
    }

    /**
     * 鍒犻櫎鏂囩珷
     */
    @PostMapping("/delete")
    @Operation(summary = "鍒犻櫎鏂囩珷")
    public BaseResponse<Boolean> deleteArticle(@RequestBody DeleteRequest deleteRequest,
                                                 HttpServletRequest httpServletRequest) {
        ThrowUtils.throwIf(deleteRequest == null || deleteRequest.getId() == null, 
                ErrorCode.PARAMS_ERROR);
        
        User loginUser = userService.getLoginUser(httpServletRequest);
        boolean result = articleService.deleteArticle(deleteRequest.getId(), loginUser);
        
        return ResultUtils.success(result);
    }

    /**
     * 鎵归噺鍒犻櫎鏂囩珷
     */
    @PostMapping("/batch-delete")
    @Operation(summary = "鎵归噺鍒犻櫎鏂囩珷")
    public BaseResponse<Integer> batchDeleteArticles(@RequestBody BatchDeleteRequest batchDeleteRequest,
                                                      HttpServletRequest httpServletRequest) {
        ThrowUtils.throwIf(batchDeleteRequest == null || batchDeleteRequest.getIds() == null
                || batchDeleteRequest.getIds().isEmpty() || batchDeleteRequest.getIds().stream().anyMatch(id -> id == null || id <= 0),
                ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(httpServletRequest);
        return ResultUtils.success(articleService.deleteArticles(batchDeleteRequest.getIds(), loginUser));
    }

    /**
     * 纭鏍囬骞惰緭鍏ヨˉ鍏呮弿杩?
     */
    @PostMapping("/confirm-title")
    @Operation(summary = "纭鏍囬骞惰緭鍏ヨˉ鍏呮弿杩?)
    public BaseResponse<Void> confirmTitle(@RequestBody ArticleConfirmTitleRequest request,
                                            HttpServletRequest httpServletRequest) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(request.getTaskId() == null || request.getTaskId().trim().isEmpty(),
                ErrorCode.PARAMS_ERROR, "浠诲姟ID涓嶈兘涓虹┖");
        ThrowUtils.throwIf(request.getSelectedMainTitle() == null || request.getSelectedMainTitle().trim().isEmpty(),
                ErrorCode.PARAMS_ERROR, "涓绘爣棰樹笉鑳戒负绌?);
        ThrowUtils.throwIf(request.getSelectedSubTitle() == null || request.getSelectedSubTitle().trim().isEmpty(),
                ErrorCode.PARAMS_ERROR, "鍓爣棰樹笉鑳戒负绌?);

        User loginUser = userService.getLoginUser(httpServletRequest);

        // 纭鏍囬
        articleService.confirmTitle(
                request.getTaskId(),
                request.getSelectedMainTitle(),
                request.getSelectedSubTitle(),
                request.getUserDescription(),
                loginUser
        );

        // 寮傛鎵ц闃舵2锛氱敓鎴愬ぇ绾?
        articleAsyncService.executePhase2(request.getTaskId());

        return ResultUtils.success(null);
    }

    /**
     * 纭澶х翰
     */
    @PostMapping("/confirm-outline")
    @Operation(summary = "纭澶х翰")
    public BaseResponse<Void> confirmOutline(@RequestBody ArticleConfirmOutlineRequest request,
                                              HttpServletRequest httpServletRequest) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(request.getTaskId() == null || request.getTaskId().trim().isEmpty(),
                ErrorCode.PARAMS_ERROR, "浠诲姟ID涓嶈兘涓虹┖");
        ThrowUtils.throwIf(request.getOutline() == null || request.getOutline().isEmpty(),
                ErrorCode.PARAMS_ERROR, "澶х翰涓嶈兘涓虹┖");

        User loginUser = userService.getLoginUser(httpServletRequest);

        // 纭澶х翰
        articleService.confirmOutline(
                request.getTaskId(),
                request.getOutline(),
                loginUser
        );

        // 寮傛鎵ц闃舵3锛氱敓鎴愭鏂?閰嶅浘
        articleAsyncService.executePhase3(request.getTaskId());

        return ResultUtils.success(null);
    }

    /**
     * AI 淇敼澶х翰
     */
    @PostMapping("/ai-modify-outline")
    @Operation(summary = "AI 淇敼澶х翰")
    public BaseResponse<List<ArticleState.OutlineSection>> aiModifyOutline(
            @RequestBody ArticleAiModifyOutlineRequest request,
            HttpServletRequest httpServletRequest) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(request.getTaskId() == null || request.getTaskId().trim().isEmpty(),
                ErrorCode.PARAMS_ERROR, "浠诲姟ID涓嶈兘涓虹┖");
        ThrowUtils.throwIf(request.getModifySuggestion() == null || request.getModifySuggestion().trim().isEmpty(),
                ErrorCode.PARAMS_ERROR, "淇敼寤鸿涓嶈兘涓虹┖");

        User loginUser = userService.getLoginUser(httpServletRequest);

        // AI 淇敼澶х翰
        List<ArticleState.OutlineSection> modifiedOutline = articleService.aiModifyOutline(
                request.getTaskId(),
                request.getModifySuggestion(),
                loginUser
        );

        return ResultUtils.success(modifiedOutline);
    }

    /**
     * 鑾峰彇浠诲姟鎵ц鏃ュ織
     */
    @GetMapping("/execution-logs/{taskId}")
    @Operation(summary = "鑾峰彇浠诲姟鎵ц鏃ュ織")
    public BaseResponse<AgentExecutionStats> getExecutionLogs(@PathVariable String taskId) {
        ThrowUtils.throwIf(taskId == null || taskId.trim().isEmpty(), 
                ErrorCode.PARAMS_ERROR, "浠诲姟ID涓嶈兘涓虹┖");
        
        AgentExecutionStats stats = agentLogService.getExecutionStats(taskId);
        return ResultUtils.success(stats);
    }
}
