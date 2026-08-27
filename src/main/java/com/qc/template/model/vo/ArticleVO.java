package com.qc.template.model.vo;

import com.google.gson.reflect.TypeToken;
import com.qc.template.model.entity.Article;
import com.qc.template.utils.GsonUtils;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 鏂囩珷瑙嗗浘
 *
 */
@Data
public class ArticleVO implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * 浠诲姟ID
     */
    private String taskId;

    /**
     * 鐢ㄦ埛ID
     */
    private Long userId;

    /**
     * 閫夐
     */
    private String topic;

    /**
     * 鐢ㄦ埛琛ュ厖鎻忚堪
     */
    private String userDescription;

    /**
     * 涓绘爣棰?
     */
    private String mainTitle;

    /**
     * 鍓爣棰?
     */
    private String subTitle;

    /**
     * 鏍囬鏂规鍒楄〃
     */
    private List<TitleOption> titleOptions;

    /**
     * 澶х翰
     */
    private List<OutlineItem> outline;

    /**
     * 姝ｆ枃
     */
    private String content;

    /**
     * 瀹屾暣鍥炬枃锛堝惈閰嶅浘锛?
     */
    private String fullContent;

    /**
     * 灏侀潰鍥?URL
     */
    private String coverImage;

    /**
     * 閰嶅浘鍒楄〃
     */
    private List<ImageItem> images;

    /**
     * 鐘舵€?
     */
    private String status;

    /**
     * 褰撳墠闃舵
     */
    private String phase;

    /**
     * 閿欒淇℃伅
     */
    private String errorMessage;

    /**
     * 鍒涘缓鏃堕棿
     */
    private LocalDateTime createTime;

    /**
     * 瀹屾垚鏃堕棿
     */
    private LocalDateTime completedTime;

    /**
     * 鏍囬鏂规
     */
    @Data
    public static class TitleOption implements Serializable {
        private String mainTitle;
        private String subTitle;
    }

    /**
     * 澶х翰椤?
     */
    @Data
    public static class OutlineItem implements Serializable {
        private Integer section;
        private String title;
        private List<String> points;
    }

    /**
     * 閰嶅浘椤?
     */
    @Data
    public static class ImageItem implements Serializable {
        private Integer position;
        private String url;
        private String method;
        private String keywords;
        private String sectionTitle;
        private String description;
        private String placeholderId;
        private String selectedVersionId;
        private List<ImageVersion> versions;
    }

    @Data
    public static class ImageVersion implements Serializable {
        private String id;
        private String url;
        private String prompt;
        private LocalDateTime createdTime;
    }

    /**
     * 瀵硅薄杞寘瑁呯被
     *
     * @param article 鏂囩珷
     * @return 鏂囩珷瑙嗗浘
     */
    public static ArticleVO objToVo(Article article) {
        if (article == null) {
            return null;
        }
        ArticleVO articleVO = new ArticleVO();
        BeanUtils.copyProperties(article, articleVO);
        
        // 杞崲 JSON 瀛楁
        if (article.getTitleOptions() != null) {
            articleVO.setTitleOptions(GsonUtils.fromJson(article.getTitleOptions(), 
                new TypeToken<List<TitleOption>>(){}));
        }
        if (article.getOutline() != null) {
            articleVO.setOutline(GsonUtils.fromJson(article.getOutline(), 
                new TypeToken<List<OutlineItem>>(){}));
        }
        if (article.getImages() != null) {
            articleVO.setImages(GsonUtils.fromJson(article.getImages(), 
                new TypeToken<List<ImageItem>>(){}));
        }
        
        return articleVO;
    }

    private static final long serialVersionUID = 1L;
}
