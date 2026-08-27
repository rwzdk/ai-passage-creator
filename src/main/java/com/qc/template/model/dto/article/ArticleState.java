package com.qc.template.model.dto.article;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 鏂囩珷鐢熸垚鐘舵€侊紙鏅鸿兘浣撻棿鍏变韩鐨勭姸鎬佸璞★級
 *
 */
@Data
public class ArticleState implements Serializable {

    /**
     * 浠诲姟ID
     */
    private String taskId;

    /**
     * 閫夐
     */
    private String topic;

    /**
     * 鐢ㄦ埛琛ュ厖鎻忚堪
     */
    private String userDescription;

    /**
     * 涓婁紶鏂囨。鐢熸垚鐨勫弬鑰冩憳瑕?     */
    private String referenceSummary;

    /**
     * 鏂囩珷椋庢牸
     */
    private String style;

    /**
     * 褰撳墠闃舵
     */
    private String phase;

    /**
     * 鏍囬鏂规鍒楄〃锛堟櫤鑳戒綋1杈撳嚭锛?
     */
    private List<TitleOption> titleOptions;

    /**
     * 鏍囬缁撴灉锛堟櫤鑳戒綋1杈撳嚭锛?
     */
    private TitleResult title;

    /**
     * 澶х翰缁撴灉锛堟櫤鑳戒綋2杈撳嚭锛?
     */
    private OutlineResult outline;

    /**
     * 姝ｆ枃鍐呭锛堟櫤鑳戒綋3杈撳嚭锛?
     */
    private String content;

    /**
     * 閰嶅浘闇€姹傚垪琛紙鏅鸿兘浣?杈撳嚭锛?
     */
    private List<ImageRequirement> imageRequirements;

    /**
     * 封面图 URL（单独存储，同时 images 列表中的 position=1 也是封面图）
     */
    private String coverImage;

    /**
     * 閰嶅浘缁撴灉鍒楄〃锛堟櫤鑳戒綋5杈撳嚭锛?
     */
    private List<ImageResult> images;

    /**
     * 鍏佽鐨勯厤鍥炬柟寮忓垪琛紙涓虹┖琛ㄧず鏀寔鎵€鏈夋柟寮忥級
     */
    private List<String> enabledImageMethods;

    /**
     * 鏍囬鏂规
     */
    @Data
    public static class TitleOption implements Serializable {
        private String mainTitle;
        private String subTitle;
    }

    /**
     * 鏍囬缁撴灉
     */
    @Data
    public static class TitleResult implements Serializable {
        private String mainTitle;
        private String subTitle;
    }

    /**
     * 澶х翰缁撴灉
     */
    @Data
    public static class OutlineResult implements Serializable {
        private List<OutlineSection> sections;
    }

    /**
     * 澶х翰绔犺妭
     */
    @Data
    public static class OutlineSection implements Serializable {
        private Integer section;
        private String title;
        private List<String> points;
    }

    /**
     * 閰嶅浘闇€姹?
     */
    @Data
    public static class ImageRequirement implements Serializable {
        private Integer position;
        private String type;
        private String sectionTitle;
        private String keywords;
        /**
         * 图片来源：PEXELS（图库检索）或 NANO_BANANA（AI 生图）
         */
        private String imageSource;
        /**
         * AI 鐢熷浘鎻愮ず璇嶏紙褰?imageSource 涓?NANO_BANANA 鏃朵娇鐢級
         */
        private String prompt;
        /**
         * 占位符ID，用于在正文中定位插入位置，格式：{{IMAGE_PLACEHOLDER_N}}
         */
        private String placeholderId;
    }

    /**
     * 閰嶅浘缁撴灉
     */
    @Data
    public static class ImageResult implements Serializable {
        private Integer position;
        private String url;
        private String method;
        private String keywords;
        private String sectionTitle;
        private String description;
        private boolean failed;
        private String error;
        /**
         * 鍗犱綅绗D锛岀敤浜庡湪姝ｆ枃涓畾浣嶆彃鍏ヤ綅缃?
         */
        private String placeholderId;
    }

    /**
     * 鏅鸿兘浣?杩斿洖缁撴灉锛堝寘鍚甫鍗犱綅绗︾殑姝ｆ枃鍜岄厤鍥鹃渶姹傚垪琛級
     */
    @Data
    public static class Agent4Result implements Serializable {
        /**
         * 鍖呭惈鍗犱綅绗︾殑姝ｆ枃鍐呭
         */
        private String contentWithPlaceholders;
        /**
         * 閰嶅浘闇€姹傚垪琛?
         */
        private List<ImageRequirement> imageRequirements;
    }

    /**
     * 瀹屾暣鍥炬枃鍐呭锛堝悎鎴愬悗锛?
     */
    private String fullContent;

    private static final long serialVersionUID = 1L;
}
