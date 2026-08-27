package com.qc.template.constant;

/**
 * 鏂囩珷鐩稿叧甯搁噺
 *
 */
public interface ArticleConstant {

    /**
     * SSE 杩炴帴瓒呮椂鏃堕棿锛堟绉掞級锛?0鍒嗛挓
     */
    long SSE_TIMEOUT_MS = 30 * 60 * 1000L;

    /**
     * SSE 閲嶈繛鏃堕棿锛堟绉掞級锛?绉?
     */
    long SSE_RECONNECT_TIME_MS = 3000L;

    // region Pexels 鐩稿叧甯搁噺

    /**
     * Pexels API 鍦板潃
     */
    String PEXELS_API_URL = "https://api.pexels.com/v1/search";

    /**
     * Pexels 姣忛〉杩斿洖鏁伴噺
     */
    int PEXELS_PER_PAGE = 1;

    /**
     * Pexels 鍥剧墖鏂瑰悜锛氭í鍚?
     */
    String PEXELS_ORIENTATION_LANDSCAPE = "landscape";

    // endregion

    // region Picsum 鐩稿叧甯搁噺

    /**
     * Picsum 闅忔満鍥剧墖 URL 妯℃澘
     */
    String PICSUM_URL_TEMPLATE = "https://picsum.photos/800/600?random=%d";

    // endregion

    // region Bing 琛ㄦ儏鍖呯浉鍏冲父閲?

    /**
     * Bing 鍥剧墖鎼滅储鍦板潃
     */
    String BING_IMAGE_SEARCH_URL = "https://cn.bing.com/images/async";

    /**
     * 琛ㄦ儏鍖呭叧閿瘝鍚庣紑锛堢▼搴忓浐瀹氭嫾鎺ワ級
     */
    String EMOJI_PACK_SUFFIX = "鐔婄尗澶磋〃鎯呭寘";

    /**
     * Bing 鍥剧墖鎼滅储姣忔壒鏈€澶ф暟閲?
     */
    int BING_MAX_IMAGES = 30;

    // endregion

    // region SVG 缁樺浘鐩稿叧甯搁噺

    /**
     * SVG 鏂囦欢鍓嶇紑
     */
    String SVG_FILE_PREFIX = "svg-chart";

    /**
     * SVG 榛樿瀹藉害
     */
    int SVG_DEFAULT_WIDTH = 800;

    /**
     * SVG 榛樿楂樺害
     */
    int SVG_DEFAULT_HEIGHT = 600;

    // endregion
}
