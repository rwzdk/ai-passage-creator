package com.qc.template.service;

import com.google.gson.reflect.TypeToken;
import com.qc.template.agent.ArticleAgentOrchestrator;
import com.qc.template.agent.config.AgentConfig;
import com.qc.template.manager.SseEmitterManager;
import com.qc.template.model.dto.article.ArticleState;
import com.qc.template.model.entity.AgentLog;
import com.qc.template.model.entity.Article;
import com.qc.template.model.enums.ArticlePhaseEnum;
import com.qc.template.model.enums.ArticleStatusEnum;
import com.qc.template.model.enums.SseMessageTypeEnum;
import com.qc.template.utils.GsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.time.LocalDateTime;

/**
 * 鏂囩珷寮傛浠诲姟鏈嶅姟
 * 
 * 鏀寔涓ょ鎵ц妯″紡锛?
 * 1. 澶氭櫤鑳戒綋缂栨帓妯″紡锛堥€氳繃 article.agent.orchestrator.enabled=true 鍚敤锛?
 * 2. 鍘熸湁妯″紡锛堥粯璁ゆ垨 article.agent.orchestrator.enabled=false锛?
 *
 */
@Service
@Slf4j
public class ArticleAsyncService {

    @Resource
    private ArticleAgentService articleAgentService;

    @Resource
    private ArticleAgentOrchestrator articleAgentOrchestrator;

    @Resource
    private AgentConfig agentConfig;

    @Resource
    private SseEmitterManager sseEmitterManager;

    @Resource
    private ArticleService articleService;

    @Resource
    private AgentLogService agentLogService;

    /**
     * 闃舵1锛氬紓姝ョ敓鎴愭爣棰樻柟妗?
     *
     * @param taskId 浠诲姟ID
     * @param topic  閫夐
     * @param style  鏂囩珷椋庢牸锛堝彲涓虹┖锛?     * @param referenceSummary 涓婁紶鏂囨。鐢熸垚鐨勫弬鑰冩憳瑕侊紙鍙负绌猴級
     */
    @Async("articleExecutor")
    public void executePhase1(String taskId, String topic, String style, String referenceSummary) {
        boolean useOrchestrator = agentConfig.isOrchestratorEnabled();
        log.info("闃舵1寮傛浠诲姟寮€濮? taskId={}, topic={}, style={}, 浣跨敤澶氭櫤鑳戒綋缂栨帓={}", 
                taskId, topic, style, useOrchestrator);
        
        try {
            // 鏇存柊鐘舵€佸拰闃舵
            articleService.updateArticleStatus(taskId, ArticleStatusEnum.PROCESSING, null);
            articleService.updatePhase(taskId, ArticlePhaseEnum.TITLE_GENERATING);
            
            // 鍒涘缓鐘舵€佸璞?
            ArticleState state = new ArticleState();
            state.setTaskId(taskId);
            state.setTopic(topic);
            state.setStyle(style);
            state.setReferenceSummary(referenceSummary);
            
            // 鎵ц闃舵1锛氱敓鎴愭爣棰樻柟妗堬紙鏍规嵁閰嶇疆閫夋嫨鎵ц鏂瑰紡锛?
            if (useOrchestrator) {
                articleAgentOrchestrator.executePhase1_GenerateTitles(state, message -> {
                    handleAgentMessage(taskId, message, state);
                });
            } else {
                articleAgentService.executePhase1_GenerateTitles(state, message -> {
                    handleAgentMessage(taskId, message, state);
                });
            }
            
            // 淇濆瓨鏍囬鏂规鍒版暟鎹簱
            articleService.saveTitleOptions(taskId, state.getTitleOptions());
            
            // 鏇存柊闃舵涓虹瓑寰呴€夋嫨鏍囬
            articleService.updatePhase(taskId, ArticlePhaseEnum.TITLE_SELECTING);
            
            // 鎺ㄩ€佹爣棰樻柟妗堢敓鎴愬畬鎴愭秷鎭?
            Map<String, Object> data = new HashMap<>();
            data.put("titleOptions", state.getTitleOptions());
            sendSseMessage(taskId, SseMessageTypeEnum.TITLES_GENERATED, data);
            
            log.info("闃舵1寮傛浠诲姟瀹屾垚, taskId={}", taskId);
        } catch (Exception e) {
            log.error("闃舵1寮傛浠诲姟澶辫触, taskId={}", taskId, e);
            
            // 鏇存柊鐘舵€佷负澶辫触
            articleService.updateArticleStatus(taskId, ArticleStatusEnum.FAILED, e.getMessage());
            
            // 鎺ㄩ€侀敊璇秷鎭?
            sendSseMessage(taskId, SseMessageTypeEnum.ERROR, Map.of("message", e.getMessage()));
            
            // 瀹屾垚 SSE 杩炴帴
            sseEmitterManager.complete(taskId);
        }
    }

    /**
     * 闃舵2锛氬紓姝ョ敓鎴愬ぇ绾诧紙鐢ㄦ埛纭鏍囬鍚庤皟鐢級
     *
     * @param taskId 浠诲姟ID
     */
    @Async("articleExecutor")
    public void executePhase2(String taskId) {
        boolean useOrchestrator = agentConfig.isOrchestratorEnabled();
        log.info("闃舵2寮傛浠诲姟寮€濮? taskId={}, 浣跨敤澶氭櫤鑳戒綋缂栨帓={}", taskId, useOrchestrator);
        
        try {
            // 鑾峰彇鏂囩珷淇℃伅
            Article article = articleService.getByTaskId(taskId);
            if (article == null) {
                throw new RuntimeException("鏂囩珷涓嶅瓨鍦?);
            }
            
            // 鍒涘缓鐘舵€佸璞?
            ArticleState state = new ArticleState();
            state.setTaskId(taskId);
            state.setStyle(article.getStyle());
            state.setUserDescription(article.getUserDescription());
            state.setReferenceSummary(article.getReferenceSummary());
            
            // 璁剧疆鏍囬
            ArticleState.TitleResult title = new ArticleState.TitleResult();
            title.setMainTitle(article.getMainTitle());
            title.setSubTitle(article.getSubTitle());
            state.setTitle(title);
            
            // 鎵ц闃舵2锛氱敓鎴愬ぇ绾诧紙鏍规嵁閰嶇疆閫夋嫨鎵ц鏂瑰紡锛?
            if (useOrchestrator) {
                articleAgentOrchestrator.executePhase2_GenerateOutline(state, message -> {
                    handleAgentMessage(taskId, message, state);
                });
            } else {
                articleAgentService.executePhase2_GenerateOutline(state, message -> {
                    handleAgentMessage(taskId, message, state);
                });
            }
            
            // 淇濆瓨澶х翰鍒版暟鎹簱
            Article articleToUpdate = articleService.getByTaskId(taskId);
            articleToUpdate.setOutline(GsonUtils.toJson(state.getOutline().getSections()));
            articleService.updateById(articleToUpdate);
            
            // 鏇存柊闃舵涓虹瓑寰呯紪杈戝ぇ绾?
            articleService.updatePhase(taskId, ArticlePhaseEnum.OUTLINE_EDITING);
            
            // 鎺ㄩ€佸ぇ绾茬敓鎴愬畬鎴愭秷鎭?
            Map<String, Object> data = new HashMap<>();
            data.put("outline", state.getOutline().getSections());
            sendSseMessage(taskId, SseMessageTypeEnum.OUTLINE_GENERATED, data);
            
            log.info("闃舵2寮傛浠诲姟瀹屾垚, taskId={}", taskId);
        } catch (Exception e) {
            log.error("闃舵2寮傛浠诲姟澶辫触, taskId={}", taskId, e);
            
            // 鏇存柊鐘舵€佷负澶辫触
            articleService.updateArticleStatus(taskId, ArticleStatusEnum.FAILED, e.getMessage());
            
            // 鎺ㄩ€侀敊璇秷鎭?
            sendSseMessage(taskId, SseMessageTypeEnum.ERROR, Map.of("message", e.getMessage()));
            
            // 瀹屾垚 SSE 杩炴帴
            sseEmitterManager.complete(taskId);
        }
    }

    /**
     * 闃舵3锛氬紓姝ョ敓鎴愭鏂?閰嶅浘锛堢敤鎴风‘璁ゅぇ绾插悗璋冪敤锛?
     *
     * @param taskId 浠诲姟ID
     */
    @Async("articleExecutor")
    public void executePhase3(String taskId) {
        boolean useOrchestrator = agentConfig.isOrchestratorEnabled();
        log.info("闃舵3寮傛浠诲姟寮€濮? taskId={}, 浣跨敤澶氭櫤鑳戒綋缂栨帓={}", taskId, useOrchestrator);
        
        try {
            // 鑾峰彇鏂囩珷淇℃伅
            Article article = articleService.getByTaskId(taskId);
            if (article == null) {
                throw new RuntimeException("鏂囩珷涓嶅瓨鍦?);
            }
            
            // 鍒涘缓鐘舵€佸璞?
            ArticleState state = new ArticleState();
            state.setTaskId(taskId);
            state.setStyle(article.getStyle());
            state.setReferenceSummary(article.getReferenceSummary());
            
            // 浠庢暟鎹簱鑾峰彇鍏佽鐨勯厤鍥炬柟寮?
            List<String> enabledMethods = null;
            if (article.getEnabledImageMethods() != null) {
                enabledMethods = GsonUtils.fromJson(
                        article.getEnabledImageMethods(),
                        new TypeToken<List<String>>(){}
                );
            }
            state.setEnabledImageMethods(enabledMethods);
            
            // 璁剧疆鏍囬
            ArticleState.TitleResult title = new ArticleState.TitleResult();
            title.setMainTitle(article.getMainTitle());
            title.setSubTitle(article.getSubTitle());
            state.setTitle(title);
            
            // 璁剧疆澶х翰
            List<ArticleState.OutlineSection> outlineSections = GsonUtils.fromJson(
                    article.getOutline(),
                    new TypeToken<List<ArticleState.OutlineSection>>(){}
            );
            ArticleState.OutlineResult outlineResult = new ArticleState.OutlineResult();
            outlineResult.setSections(outlineSections);
            state.setOutline(outlineResult);
            
            // 鎵ц闃舵3锛氱敓鎴愭鏂?閰嶅浘锛堟牴鎹厤缃€夋嫨鎵ц鏂瑰紡锛?
            // 澶氭櫤鑳戒綋缂栨帓妯″紡鏀寔閰嶅浘骞惰鐢熸垚
            if (useOrchestrator) {
                articleAgentOrchestrator.executePhase3_GenerateContent(state, message -> {
                    handleAgentMessage(taskId, message, state);
                });
            } else {
                articleAgentService.executePhase3_GenerateContent(state, message -> {
                    handleAgentMessage(taskId, message, state);
                });
            }
            
            // 淇濆瓨瀹屾暣鏂囩珷鍒版暟鎹簱
            articleService.saveArticleContent(taskId, state);
            
            // 鏇存柊鐘舵€佷负宸插畬鎴?
            articleService.updateArticleStatus(taskId, ArticleStatusEnum.COMPLETED, null);
            
            // 鎺ㄩ€佸畬鎴愭秷鎭?
            sendSseMessage(taskId, SseMessageTypeEnum.ALL_COMPLETE, Map.of("taskId", taskId));
            
            // 瀹屾垚 SSE 杩炴帴
            sseEmitterManager.complete(taskId);
            
            log.info("闃舵3寮傛浠诲姟瀹屾垚, taskId={}", taskId);
        } catch (Exception e) {
            log.error("闃舵3寮傛浠诲姟澶辫触, taskId={}", taskId, e);
            
            // 鏇存柊鐘舵€佷负澶辫触
            articleService.updateArticleStatus(taskId, ArticleStatusEnum.FAILED, e.getMessage());
            
            // 鎺ㄩ€侀敊璇秷鎭?
            sendSseMessage(taskId, SseMessageTypeEnum.ERROR, Map.of("message", e.getMessage()));
            
            // 瀹屾垚 SSE 杩炴帴
            sseEmitterManager.complete(taskId);
        }
    }

    /**
     * 澶勭悊鏅鸿兘浣撴秷鎭苟鎺ㄩ€?
     */
    private void handleAgentMessage(String taskId, String message, ArticleState state) {
        Map<String, Object> data = buildMessageData(message, state);
        if (data != null) {
            sendEvent(taskId, data);
        }
    }

    /**
     * 鏋勫缓娑堟伅鏁版嵁
     * 
     * @param message 鍘熷娑堟伅
     * @param state   鏂囩珷鐘舵€?
     * @return 娑堟伅鏁版嵁锛屽鏋滄秷鎭棤鏁堣繑鍥?null
     */
    private Map<String, Object> buildMessageData(String message, ArticleState state) {
        // 澶勭悊娴佸紡娑堟伅锛堝甫鍐掑彿鍒嗛殧绗︼級
        String streamingPrefix1 = SseMessageTypeEnum.AGENT1_STREAMING.getStreamingPrefix();
        String streamingPrefix2 = SseMessageTypeEnum.AGENT2_STREAMING.getStreamingPrefix();
        String streamingPrefix3 = SseMessageTypeEnum.AGENT3_STREAMING.getStreamingPrefix();
        String imageCompletePrefix = SseMessageTypeEnum.IMAGE_COMPLETE.getStreamingPrefix();
        String imageStartPrefix = SseMessageTypeEnum.IMAGE_START.getStreamingPrefix();
        String imageFailedPrefix = SseMessageTypeEnum.IMAGE_FAILED.getStreamingPrefix();
        String imageSkippedPrefix = SseMessageTypeEnum.IMAGE_SKIPPED.getStreamingPrefix();
        String mergeStartPrefix = SseMessageTypeEnum.MERGE_START.getStreamingPrefix();
        String imageAnalysisCompletePrefix = SseMessageTypeEnum.AGENT4_COMPLETE.getStreamingPrefix();
        String imagesCompletePrefix = SseMessageTypeEnum.AGENT5_COMPLETE.getStreamingPrefix();
        String mergeCompletePrefix = SseMessageTypeEnum.MERGE_COMPLETE.getStreamingPrefix();
        
        if (message.startsWith(streamingPrefix1)) {
            return buildStreamingData(SseMessageTypeEnum.AGENT1_STREAMING, message.substring(streamingPrefix1.length()));
        }

        if (message.startsWith(streamingPrefix2)) {
            return buildStreamingData(SseMessageTypeEnum.AGENT2_STREAMING, message.substring(streamingPrefix2.length()));
        }
        
        if (message.startsWith(streamingPrefix3)) {
            return buildStreamingData(SseMessageTypeEnum.AGENT3_STREAMING, message.substring(streamingPrefix3.length()));
        }
        
        if (message.startsWith(imageCompletePrefix)) {
            String imageJson = message.substring(imageCompletePrefix.length());
            return buildImageCompleteData(imageJson);
        }

        if (message.startsWith(imageStartPrefix)) {
            return buildImageEventData(SseMessageTypeEnum.IMAGE_START, message.substring(imageStartPrefix.length()));
        }

        if (message.startsWith(imageFailedPrefix)) {
            return buildImageEventData(SseMessageTypeEnum.IMAGE_FAILED, message.substring(imageFailedPrefix.length()));
        }

        if (message.startsWith(imageSkippedPrefix)) {
            return buildImageEventData(SseMessageTypeEnum.IMAGE_SKIPPED, message.substring(imageSkippedPrefix.length()));
        }

        if (message.startsWith(mergeStartPrefix)) {
            Map<String, Object> data = new HashMap<>();
            data.put("type", SseMessageTypeEnum.MERGE_START.getValue());
            data.putAll(GsonUtils.fromJson(message.substring(mergeStartPrefix.length()), Map.class));
            return data;
        }

        if (message.startsWith(imageAnalysisCompletePrefix)) {
            Map<String, Object> data = new HashMap<>();
            data.put("type", SseMessageTypeEnum.AGENT4_COMPLETE.getValue());
            data.put("imageRequirements", GsonUtils.fromJson(message.substring(imageAnalysisCompletePrefix.length()), Object.class));
            return data;
        }

        if (message.startsWith(imagesCompletePrefix)) {
            Map<String, Object> data = new HashMap<>();
            data.put("type", SseMessageTypeEnum.AGENT5_COMPLETE.getValue());
            data.put("images", GsonUtils.fromJson(message.substring(imagesCompletePrefix.length()), Object.class));
            return data;
        }

        if (message.startsWith(mergeCompletePrefix)) {
            Map<String, Object> data = new HashMap<>();
            data.put("type", SseMessageTypeEnum.MERGE_COMPLETE.getValue());
            data.put("fullContent", message.substring(mergeCompletePrefix.length()));
            return data;
        }
        
        // 澶勭悊瀹屾垚娑堟伅锛堟灇涓惧€硷級
        return buildCompleteMessageData(message, state);
    }

    /**
     * 鏋勫缓娴佸紡杈撳嚭鏁版嵁
     */
    private Map<String, Object> buildStreamingData(SseMessageTypeEnum type, String content) {
        Map<String, Object> data = new HashMap<>();
        data.put("type", type.getValue());
        data.put("content", content);
        return data;
    }

    /**
     * 鏋勫缓鍥剧墖瀹屾垚鏁版嵁
     */
    private Map<String, Object> buildImageCompleteData(String imageJson) {
        Map<String, Object> data = new HashMap<>();
        data.put("type", SseMessageTypeEnum.IMAGE_COMPLETE.getValue());
        data.put("image", GsonUtils.fromJson(imageJson, ArticleState.ImageResult.class));
        return data;
    }

    private Map<String, Object> buildImageEventData(SseMessageTypeEnum type, String imageJson) {
        Map<String, Object> data = new HashMap<>();
        data.put("type", type.getValue());
        data.put("image", GsonUtils.fromJson(imageJson, Object.class));
        return data;
    }

    /**
     * 鏋勫缓瀹屾垚娑堟伅鏁版嵁
     */
    private Map<String, Object> buildCompleteMessageData(String message, ArticleState state) {
        Map<String, Object> data = new HashMap<>();
        
        // 浣跨敤鏋氫妇鍊煎尮閰?
        if (SseMessageTypeEnum.AGENT1_COMPLETE.getValue().equals(message)) {
            data.put("type", SseMessageTypeEnum.AGENT1_COMPLETE.getValue());
            data.put("title", state.getTitle());
        } else if (SseMessageTypeEnum.AGENT2_COMPLETE.getValue().equals(message)) {
            data.put("type", SseMessageTypeEnum.AGENT2_COMPLETE.getValue());
            data.put("outline", state.getOutline().getSections());
        } else if (SseMessageTypeEnum.AGENT3_COMPLETE.getValue().equals(message)) {
            data.put("type", SseMessageTypeEnum.AGENT3_COMPLETE.getValue());
        } else if (SseMessageTypeEnum.AGENT4_START.getValue().equals(message)) {
            data.put("type", SseMessageTypeEnum.AGENT4_START.getValue());
        } else if (SseMessageTypeEnum.AGENT4_COMPLETE.getValue().equals(message)) {
            data.put("type", SseMessageTypeEnum.AGENT4_COMPLETE.getValue());
            data.put("imageRequirements", state.getImageRequirements());
        } else if (SseMessageTypeEnum.AGENT5_COMPLETE.getValue().equals(message)) {
            data.put("type", SseMessageTypeEnum.AGENT5_COMPLETE.getValue());
            data.put("images", state.getImages());
        } else if (SseMessageTypeEnum.MERGE_COMPLETE.getValue().equals(message)) {
            data.put("type", SseMessageTypeEnum.MERGE_COMPLETE.getValue());
            data.put("fullContent", state.getFullContent());
        } else if (SseMessageTypeEnum.MERGE_START.getValue().equals(message)) {
            data.put("type", SseMessageTypeEnum.MERGE_START.getValue());
        } else {
            return null;
        }
        
        return data;
    }

    /**
     * 鍙戦€?SSE 娑堟伅
     */
    private void sendSseMessage(String taskId, SseMessageTypeEnum type, Map<String, Object> additionalData) {
        Map<String, Object> data = new HashMap<>();
        data.put("type", type.getValue());
        data.putAll(additionalData);
        sendEvent(taskId, data);
    }

    /** 鎺ㄩ€佸苟鎸佷箙鍖栫敤鎴峰彲瑙佺殑 SSE 浜嬩欢锛屼緵鍘嗗彶鍒涗綔椤靛畬鏁村洖鏀俱€?*/
    private void sendEvent(String taskId, Map<String, Object> data) {
        String payload = GsonUtils.toJson(data);
        String type = String.valueOf(data.get("type"));
        // 娴佸紡姝ｆ枃/澶х翰姣忎釜 token 閮戒細瑙﹀彂浜嬩欢锛屽疄鏃堕潰鏉挎湰韬笉灞曠ず杩欎簺 token锛屽巻鍙蹭篃鏃犻渶鎸佷箙鍖栥€?        if (!SseMessageTypeEnum.AGENT1_STREAMING.getValue().equals(type)
                && !SseMessageTypeEnum.AGENT2_STREAMING.getValue().equals(type)
                && !SseMessageTypeEnum.AGENT3_STREAMING.getValue().equals(type)) {
            LocalDateTime now = LocalDateTime.now();
            AgentLog eventLog = AgentLog.builder()
                    .taskId(taskId)
                    .agentName("__event_" + type)
                    .startTime(now)
                    .endTime(now)
                    .status("SUCCESS")
                    .outputData(payload)
                    .build();
            agentLogService.saveLogAsync(eventLog);
        }
        sseEmitterManager.send(taskId, payload);
    }
}
