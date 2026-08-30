package com.qc.template.model.enums;

import lombok.Getter;

@Getter
public enum SseMessageTypeEnum {
    AGENT1_COMPLETE("AGENT1_COMPLETE", "标题方案生成完成"),
    AGENT1_STREAMING("AGENT1_STREAMING", "标题方案流式输出"),
    TITLES_GENERATED("TITLES_GENERATED", "标题方案已生成"),
    AGENT2_STREAMING("AGENT2_STREAMING", "大纲流式输出"),
    AGENT2_COMPLETE("AGENT2_COMPLETE", "大纲生成完成"),
    OUTLINE_GENERATED("OUTLINE_GENERATED", "大纲已生成"),
    AGENT3_STREAMING("AGENT3_STREAMING", "正文流式输出"),
    AGENT3_COMPLETE("AGENT3_COMPLETE", "正文生成完成"),
    AGENT4_START("AGENT4_START", "开始分析配图需求"),
    AGENT4_COMPLETE("AGENT4_COMPLETE", "配图需求分析完成"),
    IMAGE_COMPLETE("IMAGE_COMPLETE", "单张配图完成"),
    IMAGE_START("IMAGE_START", "开始生成配图"),
    IMAGE_FAILED("IMAGE_FAILED", "配图生成失败"),
    IMAGE_SKIPPED("IMAGE_SKIPPED", "配图已跳过"),
    AGENT5_COMPLETE("AGENT5_COMPLETE", "配图生成完成"),
    MERGE_START("MERGE_START", "开始合成图文"),
    MERGE_COMPLETE("MERGE_COMPLETE", "图文合成完成"),
    ALL_COMPLETE("ALL_COMPLETE", "全部完成"),
    ERROR("ERROR", "处理失败");

    private final String value;
    private final String description;

    SseMessageTypeEnum(String value, String description) {
        this.value = value;
        this.description = description;
    }

    public String getStreamingPrefix() {
        return this.value + ":";
    }
}
