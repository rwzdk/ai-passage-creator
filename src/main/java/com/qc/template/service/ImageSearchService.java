package com.qc.template.service;

import com.qc.template.model.dto.image.ImageData;
import com.qc.template.model.dto.image.ImageRequest;
import com.qc.template.model.enums.ImageMethodEnum;

/**
 * 鍥剧墖鏈嶅姟鎺ュ彛
 * 鎶借薄鍥剧墖鑾峰彇閫昏緫锛屼究浜庢墿灞曞绉嶅浘鐗囨潵婧愶紙濡?Pexels銆乁nsplash銆丄I 鐢熷浘绛夛級
 * 
 * 鎵╁睍鏂扮殑鍥剧墖鏈嶅姟鏃讹細
 * 1. 瀹炵幇姝ゆ帴鍙?
 * 2. 鍦?ImageMethodEnum 涓坊鍔犲搴旂殑鏋氫妇鍊?
 * 3. 娣诲姞瀵瑰簲鐨勯厤缃被锛堝闇€瑕侊級
 *
 */
public interface ImageSearchService {

    /**
     * 鏍规嵁璇锋眰鑾峰彇鍥剧墖锛堟帹鑽愪娇鐢ㄦ鏂规硶锛?
     * 
     * @param request 鍥剧墖璇锋眰瀵硅薄锛屽寘鍚?keywords銆乸rompt 绛夊弬鏁?
     * @return 鍥剧墖 URL锛岃幏鍙栧け璐ヨ繑鍥?null
     */
    default String getImage(ImageRequest request) {
        // 榛樿瀹炵幇锛氭牴鎹湇鍔＄被鍨嬮€夋嫨鍚堥€傜殑鍙傛暟
        String param = request.getEffectiveParam(getMethod().isAiGenerated());
        return searchImage(param);
    }

    /**
     * 鑾峰彇鍥剧墖鏁版嵁锛堢敤浜庣粺涓€涓婁紶鍒?COS锛?
     * 瀛愮被鍙噸鍐欐鏂规硶杩斿洖鏇撮珮鏁堢殑鏁版嵁鏍煎紡锛堝瀛楄妭鏁版嵁锛?
     *
     * @param request 图片请求对象
     * @return ImageData 对象，包含图片字节或 URL
     */
    default ImageData getImageData(ImageRequest request) {
        // 榛樿瀹炵幇锛氶€氳繃 getImage 鑾峰彇 URL锛岀劧鍚庤浆鎹负 ImageData
        String url = getImage(request);
        return ImageData.fromUrl(url);
    }

    /**
     * 鏍规嵁鍏抽敭璇?鎻愮ず璇嶈幏鍙栧浘鐗?
     * 
     * @param keywords 鎼滅储鍏抽敭璇嶏紙鍥惧簱妫€绱級鎴栫敓鍥炬彁绀鸿瘝锛圓I 鐢熷浘锛?
     * @return 鍥剧墖 URL锛岃幏鍙栧け璐ヨ繑鍥?null
     */
    String searchImage(String keywords);

    /**
     * 鑾峰彇鍥剧墖鏈嶅姟绫诲瀷
     *
     * @return 鍥剧墖鏈嶅姟绫诲瀷鏋氫妇
     */
    ImageMethodEnum getMethod();

    /**
     * 鑾峰彇闄嶇骇鍥剧墖 URL
     *
     * @param position 浣嶇疆搴忓彿锛堢敤浜庣敓鎴愬敮涓€鐨勯殢鏈哄浘鐗囷級
     * @return 闄嶇骇鍥剧墖 URL
     */
    String getFallbackImage(int position);

    /**
     * 鍒ゆ柇鏈嶅姟鏄惁鍙敤
     * 瀛愮被鍙噸鍐欐鏂规硶杩涜鍋ュ悍妫€鏌?
     *
     * @return 鏈嶅姟鏄惁鍙敤
     */
    default boolean isAvailable() {
        return true;
    }
}
