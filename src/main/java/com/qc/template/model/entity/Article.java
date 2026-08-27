package com.qc.template.model.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 鏂囩珷瀹炰綋绫?
 *
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(value = "article", camelToUnderline = false)
public class Article implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    @Id(keyType = KeyType.Auto)
    private Long id;

    /**
     * 浠诲姟ID锛圲UID锛?
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
     * 涓婁紶鏂囨。鐢熸垚鐨勫弬鑰冩憳瑕侊紙涓嶄繚瀛樺師鏂囦欢锛?     */
    private String referenceSummary;

    /**
     * 鍏佽鐨勯厤鍥炬柟寮忓垪琛紙JSON鏍煎紡锛?
     */
    private String enabledImageMethods;

    /**
     * 鏂囩珷椋庢牸锛歵ech/emotional/educational/humorous锛屽彲涓虹┖
     */
    private String style;

    /**
     * 涓绘爣棰?
     */
    private String mainTitle;

    /**
     * 鍓爣棰?
     */
    private String subTitle;

    /**
     * 鏍囬鏂规鍒楄〃锛圝SON鏍煎紡锛?
     */
    private String titleOptions;

    /**
     * 澶х翰锛圝SON鏍煎紡锛?
     */
    private String outline;

    /**
     * 姝ｆ枃锛圡arkdown鏍煎紡锛屼笉鍚浘鐗囷級
     */
    private String content;

    /**
     * 瀹屾暣鍥炬枃锛圡arkdown鏍煎紡锛屽惈鍥剧墖锛?
     */
    private String fullContent;

    /**
     * 灏侀潰鍥?URL
     */
    private String coverImage;

    /**
     * 閰嶅浘鍒楄〃锛圝SON鏁扮粍锛屽寘鍚皝闈㈠浘 position=1锛?
     */
    private String images;

    /**
     * 鐘舵€侊細PENDING/PROCESSING/COMPLETED/FAILED
     */
    private String status;

    /**
     * 褰撳墠闃舵锛歅ENDING/TITLE_GENERATING/TITLE_SELECTING/OUTLINE_GENERATING/OUTLINE_EDITING/CONTENT_GENERATING
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
     * 鏇存柊鏃堕棿
     */
    private LocalDateTime updateTime;

    /**
     * 鏄惁鍒犻櫎
     */
    @Column(isLogicDelete = true)
    private Integer isDelete;

}
