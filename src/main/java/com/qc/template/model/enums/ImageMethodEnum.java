package com.qc.template.model.enums;

import lombok.Getter;

@Getter
public enum ImageMethodEnum {
    PEXELS("PEXELS", "Pexels 图片搜索", false, false),
    NANO_BANANA("NANO_BANANA", "Nano Banana AI 生图", true, false),
    NANO_BANANA_APICLAUDE("NANO_BANANA_APICLAUDE", "通过 apiclaude 接入 Nano Banana", true, false),
    IMAGE_2("IMAGE_2", "GPT Image 2 AI 生图", true, false),
    MERMAID("MERMAID", "Mermaid 结构图", true, false),
    ICONIFY("ICONIFY", "Iconify 图标", false, false),
    EMOJI_PACK("EMOJI_PACK", "Emoji 图片包", false, false),
    SVG_DIAGRAM("SVG_DIAGRAM", "SVG 示意图", true, false),
    PICSUM("PICSUM", "Picsum 随机图片", false, true);

    private final String value;
    private final String description;
    private final boolean aiGenerated;
    private final boolean fallback;

    ImageMethodEnum(String value, String description, boolean aiGenerated, boolean fallback) {
        this.value = value;
        this.description = description;
        this.aiGenerated = aiGenerated;
        this.fallback = fallback;
    }

    public static ImageMethodEnum getByValue(String value) {
        if (value == null) {
            return null;
        }
        for (ImageMethodEnum methodEnum : values()) {
            if (methodEnum.getValue().equals(value)) {
                return methodEnum;
            }
        }
        return null;
    }

    public static ImageMethodEnum getDefaultSearchMethod() {
        return PEXELS;
    }

    public static ImageMethodEnum getDefaultAiMethod() {
        return NANO_BANANA;
    }

    public static ImageMethodEnum getFallbackMethod() {
        return PICSUM;
    }
}
