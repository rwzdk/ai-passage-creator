package com.qc.template.config;

import com.qc.template.constant.PromptConstant;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

/**
 * Prompt 閰嶇疆绫?
 * 鏀寔閫氳繃閰嶇疆鏂囦欢瑕嗙洊榛樿 Prompt
 *
 */
@Configuration
@ConfigurationProperties(prefix = "prompt")
@Data
public class PromptConfig {

    /**
     * Prompt 鐗堟湰鍙?
     */
    private String version = "1.0";

    /**
     * Prompt 妯℃澘鏄犲皠
     */
    private Map<String, String> templates = new HashMap<>();

    @PostConstruct
    public void init() {
        // 浠?PromptConstant 鍒濆鍖栭粯璁ゅ€?
        templates.putIfAbsent("agent1_title", PromptConstant.AGENT1_TITLE_PROMPT);
        templates.putIfAbsent("agent2_outline", PromptConstant.AGENT2_OUTLINE_PROMPT);
        templates.putIfAbsent("agent3_content", PromptConstant.AGENT3_CONTENT_PROMPT);
        templates.putIfAbsent("agent4_image", PromptConstant.AGENT4_IMAGE_REQUIREMENTS_PROMPT);
        templates.putIfAbsent("ai_modify_outline", PromptConstant.AI_MODIFY_OUTLINE_PROMPT);
    }

    /**
     * 鑾峰彇 Prompt 妯℃澘
     *
     * @param key Prompt 閿悕
     * @return Prompt 鍐呭
     */
    public String getPrompt(String key) {
        return templates.getOrDefault(key, "");
    }
}
