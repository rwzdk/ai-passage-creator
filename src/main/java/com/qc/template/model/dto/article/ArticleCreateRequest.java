package com.qc.template.model.dto.article;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 鍒涘缓鏂囩珷璇锋眰
 *
 */
@Data
public class ArticleCreateRequest implements Serializable {

    /**
     * 閫夐
     */
    private String topic;

    /**
     * 鏂囩珷椋庢牸锛歵ech/emotional/educational/humorous锛屽彲涓虹┖
     */
    private String style;

    /**
     * 鍏佽鐨勯厤鍥炬柟寮忓垪琛紙涓虹┖鎴?null 琛ㄧず鏀寔鎵€鏈夋柟寮忥級
     * 鍙€夊€硷細PEXELS, NANO_BANANA, MERMAID, ICONIFY, EMOJI_PACK, SVG_DIAGRAM
     */
    private List<String> enabledImageMethods;

    /**
     * 涓婁紶鏂囨。鐢熸垚鐨勫弬鑰冩憳瑕侊紝鍙负绌?     */
    private String referenceSummary;

    private static final long serialVersionUID = 1L;
}
