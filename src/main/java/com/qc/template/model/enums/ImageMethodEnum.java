package com.qc.template.model.enums;

import lombok.Getter;

/**
 * 閰嶅浘鏂瑰紡鏋氫妇
 * 
 * 鎵╁睍鏂扮殑鍥剧墖鏉ユ簮鏃讹紝鍙渶娣诲姞鏂扮殑鏋氫妇鍊煎苟璁剧疆姝ｇ‘鐨勫睘鎬э細
 * - isAiGenerated: 鏄惁涓?AI 鐢熷浘鏂瑰紡锛堝喅瀹氫娇鐢?prompt 杩樻槸 keywords锛?
 * - isFallback: 鏄惁涓洪檷绾ф柟妗?
 *
 */
@Getter
public enum ImageMethodEnum {

    /**
     * Pexels 鍥惧簱妫€绱?
     */
    PEXELS("PEXELS", "Pexels 鍥惧簱", false, false),

    /**
     * Nano Banana AI 鐢熷浘锛圙emini 鍘熺敓鍥剧墖鐢熸垚锛?
     */
    NANO_BANANA("NANO_BANANA", "Nano Banana AI 鐢熷浘", true, false),

    NANO_BANANA_APICLAUDE("NANO_BANANA_APICLAUDE", "Nano Banana锛坅piclaude锛?, true, false),

    /**
     * GPT Image 2 AI 鐢熷浘
     */
    IMAGE_2("IMAGE_2", "GPT Image 2 AI 鐢熷浘", true, false),

    /**
     * Mermaid 娴佺▼鍥剧敓鎴?
     */
    MERMAID("MERMAID", "Mermaid 娴佺▼鍥剧敓鎴?, true, false),

    /**
     * Iconify 鍥炬爣搴撴绱?
     */
    ICONIFY("ICONIFY", "Iconify 鍥炬爣搴?, false, false),

    /**
     * 琛ㄦ儏鍖呮绱紙Bing 鍥剧墖鎼滅储锛?
     */
    EMOJI_PACK("EMOJI_PACK", "琛ㄦ儏鍖呮绱?, false, false),

    /**
     * SVG 姒傚康绀烘剰鍥剧敓鎴愶紙AI 鐢熸垚 SVG 浠ｇ爜锛?
     */
    SVG_DIAGRAM("SVG_DIAGRAM", "SVG 姒傚康绀烘剰鍥?, true, false),

    /**
     * Picsum 闅忔満鍥剧墖锛堥檷绾ф柟妗堬級
     */
    PICSUM("PICSUM", "Picsum 闅忔満鍥剧墖", false, true);

    // ============ 鎵╁睍绀轰緥 ============
    // DALL_E("DALL_E", "DALL-E AI 鐢熷浘", true, false),
    // MIDJOURNEY("MIDJOURNEY", "Midjourney AI 鐢熷浘", true, false),
    // UNSPLASH("UNSPLASH", "Unsplash 鍥惧簱", false, false),
    // STABLE_DIFFUSION("STABLE_DIFFUSION", "Stable Diffusion AI 鐢熷浘", true, false),

    /**
     * 鏂规硶鍊?
     */
    private final String value;

    /**
     * 鏂规硶鎻忚堪
     */
    private final String description;

    /**
     * 鏄惁涓?AI 鐢熷浘鏂瑰紡
     * true: 浣跨敤 prompt 鐢熸垚鍥剧墖锛堝 DALL-E銆丮idjourney銆丯ano Banana锛?
     * false: 浣跨敤 keywords 妫€绱㈠浘鐗囷紙濡?Pexels銆乁nsplash锛?
     */
    private final boolean aiGenerated;

    /**
     * 鏄惁涓洪檷绾ф柟妗?
     */
    private final boolean fallback;

    ImageMethodEnum(String value, String description, boolean aiGenerated, boolean fallback) {
        this.value = value;
        this.description = description;
        this.aiGenerated = aiGenerated;
        this.fallback = fallback;
    }

    /**
     * 鏍规嵁鍊艰幏鍙栨灇涓?
     *
     * @param value 鏂规硶鍊?
     * @return 鏋氫妇瀹炰緥
     */
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

    /**
     * 鑾峰彇榛樿鐨勫浘搴撴绱㈡柟寮?
     */
    public static ImageMethodEnum getDefaultSearchMethod() {
        return PEXELS;
    }

    /**
     * 鑾峰彇榛樿鐨?AI 鐢熷浘鏂瑰紡
     */
    public static ImageMethodEnum getDefaultAiMethod() {
        return NANO_BANANA;
    }

    /**
     * 鑾峰彇闄嶇骇鏂规
     */
    public static ImageMethodEnum getFallbackMethod() {
        return PICSUM;
    }
}
