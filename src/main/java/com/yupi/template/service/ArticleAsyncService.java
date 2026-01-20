package com.yupi.template.service;

import com.yupi.template.manager.SseEmitterManager;
import com.yupi.template.model.dto.article.ArticleState;
import com.yupi.template.model.enums.ArticleStatusEnum;
import com.yupi.template.model.enums.SseMessageTypeEnum;
import com.yupi.template.utils.GsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

/**
 * 文章异步任务服务
 *
 * @author <a href="https://codefather.cn">编程导航学习圈</a>
 */
@Service
@Slf4j
public class ArticleAsyncService {

    @Resource
    private ArticleAgentService articleAgentService;

    @Resource
    private SseEmitterManager sseEmitterManager;

    @Resource
    private ArticleService articleService;

    /**
     * 异步执行文章生成
     *
     * @param taskId              任务ID
     * @param topic               选题
     * @param style               文章风格（可为空）
     * @param enabledImageMethods 允许的配图方式列表（为空表示支持所有方式）
     */
    @Async("articleExecutor")
    public void executeArticleGeneration(String taskId, String topic, String style, java.util.List<String> enabledImageMethods) {
        log.info("异步任务开始, taskId={}, topic={}, style={}, enabledImageMethods={}", taskId, topic, style, enabledImageMethods);
        
        try {
            // 更新状态为处理中
            articleService.updateArticleStatus(taskId, ArticleStatusEnum.PROCESSING, null);
            
            // 创建状态对象
            ArticleState state = new ArticleState();
            state.setTaskId(taskId);
            state.setTopic(topic);
            state.setStyle(style);
            state.setEnabledImageMethods(enabledImageMethods);
            
            // 执行智能体编排,并通过 SSE 推送进度
            articleAgentService.executeArticleGeneration(state, message -> {
                handleAgentMessage(taskId, message, state);
            });
            
            // 保存完整文章到数据库
            articleService.saveArticleContent(taskId, state);
            
            // 更新状态为已完成
            articleService.updateArticleStatus(taskId, ArticleStatusEnum.COMPLETED, null);
            
            // 推送完成消息
            sendSseMessage(taskId, SseMessageTypeEnum.ALL_COMPLETE, Map.of("taskId", taskId));
            
            // 完成 SSE 连接
            sseEmitterManager.complete(taskId);
            
            log.info("异步任务完成, taskId={}", taskId);
        } catch (Exception e) {
            log.error("异步任务失败, taskId={}", taskId, e);
            
            // 更新状态为失败
            articleService.updateArticleStatus(taskId, ArticleStatusEnum.FAILED, e.getMessage());
            
            // 推送错误消息
            sendSseMessage(taskId, SseMessageTypeEnum.ERROR, Map.of("message", e.getMessage()));
            
            // 完成 SSE 连接
            sseEmitterManager.complete(taskId);
        }
    }

    /**
     * 处理智能体消息并推送
     */
    private void handleAgentMessage(String taskId, String message, ArticleState state) {
        Map<String, Object> data = buildMessageData(message, state);
        if (data != null) {
            sseEmitterManager.send(taskId, GsonUtils.toJson(data));
        }
    }

    /**
     * 构建消息数据
     * 
     * @param message 原始消息
     * @param state   文章状态
     * @return 消息数据，如果消息无效返回 null
     */
    private Map<String, Object> buildMessageData(String message, ArticleState state) {
        // 处理流式消息（带冒号分隔符）
        String streamingPrefix2 = SseMessageTypeEnum.AGENT2_STREAMING.getStreamingPrefix();
        String streamingPrefix3 = SseMessageTypeEnum.AGENT3_STREAMING.getStreamingPrefix();
        String imageCompletePrefix = SseMessageTypeEnum.IMAGE_COMPLETE.getStreamingPrefix();
        
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
        
        // 处理完成消息（枚举值）
        return buildCompleteMessageData(message, state);
    }

    /**
     * 构建流式输出数据
     */
    private Map<String, Object> buildStreamingData(SseMessageTypeEnum type, String content) {
        Map<String, Object> data = new HashMap<>();
        data.put("type", type.getValue());
        data.put("content", content);
        return data;
    }

    /**
     * 构建图片完成数据
     */
    private Map<String, Object> buildImageCompleteData(String imageJson) {
        Map<String, Object> data = new HashMap<>();
        data.put("type", SseMessageTypeEnum.IMAGE_COMPLETE.getValue());
        data.put("image", GsonUtils.fromJson(imageJson, ArticleState.ImageResult.class));
        return data;
    }

    /**
     * 构建完成消息数据
     */
    private Map<String, Object> buildCompleteMessageData(String message, ArticleState state) {
        Map<String, Object> data = new HashMap<>();
        
        // 使用枚举值匹配
        if (SseMessageTypeEnum.AGENT1_COMPLETE.getValue().equals(message)) {
            data.put("type", SseMessageTypeEnum.AGENT1_COMPLETE.getValue());
            data.put("title", state.getTitle());
        } else if (SseMessageTypeEnum.AGENT2_COMPLETE.getValue().equals(message)) {
            data.put("type", SseMessageTypeEnum.AGENT2_COMPLETE.getValue());
            data.put("outline", state.getOutline().getSections());
        } else if (SseMessageTypeEnum.AGENT3_COMPLETE.getValue().equals(message)) {
            data.put("type", SseMessageTypeEnum.AGENT3_COMPLETE.getValue());
        } else if (SseMessageTypeEnum.AGENT4_COMPLETE.getValue().equals(message)) {
            data.put("type", SseMessageTypeEnum.AGENT4_COMPLETE.getValue());
            data.put("imageRequirements", state.getImageRequirements());
        } else if (SseMessageTypeEnum.AGENT5_COMPLETE.getValue().equals(message)) {
            data.put("type", SseMessageTypeEnum.AGENT5_COMPLETE.getValue());
            data.put("images", state.getImages());
        } else if (SseMessageTypeEnum.MERGE_COMPLETE.getValue().equals(message)) {
            data.put("type", SseMessageTypeEnum.MERGE_COMPLETE.getValue());
            data.put("fullContent", state.getFullContent());
        } else {
            return null;
        }
        
        return data;
    }

    /**
     * 发送 SSE 消息
     */
    private void sendSseMessage(String taskId, SseMessageTypeEnum type, Map<String, Object> additionalData) {
        Map<String, Object> data = new HashMap<>();
        data.put("type", type.getValue());
        data.putAll(additionalData);
        sseEmitterManager.send(taskId, GsonUtils.toJson(data));
    }
}
