package com.qc.template.model.dto.image;

import lombok.Builder;
import lombok.Data;

/**
 * 鍥剧墖璇锋眰瀵硅薄
 * 缁熶竴灏佽鍥剧墖鑾峰彇鎵€闇€鐨勫悇绉嶅弬鏁帮紝渚夸簬鎵╁睍
 *
 */
@Data
@Builder
public class ImageRequest {

    /**
     * 鎼滅储鍏抽敭璇嶏紙鐢ㄤ簬鍥惧簱妫€绱級
     */
    private String keywords;

    /**
     * 鐢熷浘鎻愮ず璇嶏紙鐢ㄤ簬 AI 鐢熷浘锛?
     */
    private String prompt;

    /**
     * 鍥剧墖浣嶇疆搴忓彿
     */
    private Integer position;

    /**
     * 鍥剧墖绫诲瀷锛坈over/section锛?
     */
    private String type;

    /**
     * 瀹介珮姣旓紙濡?16:9, 1:1锛?
     */
    private String aspectRatio;

    /**
     * 鍥剧墖椋庢牸鎻忚堪
     */
    private String style;

    /**
     * 鍥炬敼鍥剧殑鍘熷浘鍏綉 URL锛涗负绌烘椂浣跨敤鏂囩敓鍥俱€?     */
    private String sourceImageUrl;

    /**
     * 鑾峰彇鏈夋晥鐨勬悳绱?鐢熸垚鍙傛暟
     * AI 鐢熷浘浼樺厛浣跨敤 prompt锛屽浘搴撴绱娇鐢?keywords
     *
     * @param isAiGenerated 鏄惁涓?AI 鐢熷浘鏂瑰紡
     * @return 鏈夋晥鐨勫弬鏁?
     */
    public String getEffectiveParam(boolean isAiGenerated) {
        if (isAiGenerated) {
            return prompt != null && !prompt.isEmpty() ? prompt : keywords;
        }
        return keywords != null && !keywords.isEmpty() ? keywords : prompt;
    }
}
