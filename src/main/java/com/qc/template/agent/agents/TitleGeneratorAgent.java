package com.qc.template.agent.agents;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.google.gson.reflect.TypeToken;
import com.qc.template.constant.PromptConstant;
import com.qc.template.annotation.AgentExecution;
import com.qc.template.agent.context.StreamHandlerContext;
import com.qc.template.model.dto.article.ArticleState;
import com.qc.template.model.enums.ArticleStyleEnum;
import com.qc.template.model.enums.SseMessageTypeEnum;
import com.qc.template.utils.GsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 标题生成 Agent
 * 根据选题生成 3-5 个爆款标题方案
 *
 * @author YuanJian Studio
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class TitleGeneratorAgent implements NodeAction {

    private final DashScopeChatModel chatModel;

    public static final String INPUT_TOPIC = "topic";
    public static final String INPUT_STYLE = "style";
    public static final String INPUT_REFERENCE_SUMMARY = "referenceSummary";
    public static final String OUTPUT_TITLE_OPTIONS = "titleOptions";

    @Override
    @AgentExecution(value = "agent1_generate_titles", description = "生成标题方案")
    public Map<String, Object> apply(OverAllState state) throws Exception {
        String topic = state.value(INPUT_TOPIC)
                .map(Object::toString)
                .orElseThrow(() -> new IllegalArgumentException("缺少选题参数"));
        
        String style = state.value(INPUT_STYLE)
                .map(Object::toString)
                .orElse(null);
        String referenceSummary = state.value(INPUT_REFERENCE_SUMMARY)
                .map(Object::toString)
                .orElse(null);
        
        log.info("TitleGeneratorAgent 开始执行: topic={}, style={}", topic, style);
        
        // 构建 prompt
        String prompt = PromptConstant.AGENT1_TITLE_PROMPT
                .replace("{topic}", topic)
                + referenceSection(referenceSummary)
                + getStylePrompt(style);
        
        Consumer<String> streamHandler = StreamHandlerContext.get();
        String content = callLlmWithStreaming(prompt, streamHandler);
        
        // 解析结果
        List<ArticleState.TitleOption> titleOptions = GsonUtils.fromJson(
                content,
                new TypeToken<List<ArticleState.TitleOption>>(){}
        );
        
        log.info("TitleGeneratorAgent 执行完成: 生成了 {} 个标题方案", titleOptions.size());
        
        return Map.of(OUTPUT_TITLE_OPTIONS, titleOptions);
    }

    private String callLlmWithStreaming(String prompt, Consumer<String> streamHandler) {
        StringBuilder contentBuilder = new StringBuilder();
        Flux<ChatResponse> streamResponse = chatModel.stream(new Prompt(new UserMessage(prompt)));

        streamResponse
                .doOnNext(response -> {
                    String chunk = response.getResult().getOutput().getText();
                    if (chunk != null && !chunk.isEmpty()) {
                        contentBuilder.append(chunk);
                        if (streamHandler != null) {
                            streamHandler.accept(
                                    SseMessageTypeEnum.AGENT1_STREAMING.getStreamingPrefix() + chunk
                            );
                        }
                    }
                })
                .doOnError(error -> log.error("TitleGeneratorAgent 流式调用失败", error))
                .blockLast();

        return contentBuilder.toString();
    }

    private String referenceSection(String referenceSummary) {
        if (referenceSummary == null || referenceSummary.isBlank()) {
            return "";
        }
        return PromptConstant.REFERENCE_SUMMARY_SECTION.replace("{referenceSummary}", referenceSummary);
    }

    /**
     * 根据风格获取对应的 Prompt 附加内容
     */
    private String getStylePrompt(String style) {
        if (style == null || style.isEmpty()) {
            return "";
        }
        
        ArticleStyleEnum styleEnum = ArticleStyleEnum.getEnumByValue(style);
        if (styleEnum == null) {
            return "";
        }
        
        return switch (styleEnum) {
            case TECH -> PromptConstant.STYLE_TECH_PROMPT;
            case EMOTIONAL -> PromptConstant.STYLE_EMOTIONAL_PROMPT;
            case EDUCATIONAL -> PromptConstant.STYLE_EDUCATIONAL_PROMPT;
            case HUMOROUS -> PromptConstant.STYLE_HUMOROUS_PROMPT;
        };
    }
}
