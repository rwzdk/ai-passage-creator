package com.qc.template.model.dto.image;

import lombok.Builder;
import lombok.Data;

import java.util.Base64;

/**
 * 鍥剧墖鏁版嵁灏佽绫?
 * 用于统一处理不同来源的图片数据（字节、URL、base64 等）
 *
 */
@Data
@Builder
public class ImageData {

    /**
     * 鍥剧墖瀛楄妭鏁版嵁
     */
    private byte[] bytes;

    /**
     * 鍥剧墖 URL锛堝閮?URL 鎴?base64 data URL锛?
     */
    private String url;

    /**
     * MIME 绫诲瀷锛堝 image/png, image/jpeg, image/svg+xml锛?
     */
    private String mimeType;

    /**
     * 鏁版嵁绫诲瀷
     */
    private DataType dataType;

    /**
     * 鏁版嵁绫诲瀷鏋氫妇
     */
    public enum DataType {
        /**
         * 瀛楄妭鏁版嵁
         */
        BYTES,
        /**
         * 澶栭儴 URL
         */
        URL,
        /**
         * base64 data URL
         */
        DATA_URL
    }

    /**
     * 浠庡閮?URL 鍒涘缓 ImageData
     *
     * @param url 澶栭儴 URL
     * @return ImageData 瀹炰緥
     */
    public static ImageData fromUrl(String url) {
        if (url == null || url.isEmpty()) {
            return null;
        }
        
        // 鍒ゆ柇鏄惁涓?base64 data URL
        if (url.startsWith("data:")) {
            return fromDataUrl(url);
        }
        
        return ImageData.builder()
                .url(url)
                .dataType(DataType.URL)
                .build();
    }

    /**
     * 浠?base64 data URL 鍒涘缓 ImageData
     *
     * @param dataUrl base64 data URL
     * @return ImageData 瀹炰緥
     */
    public static ImageData fromDataUrl(String dataUrl) {
        if (dataUrl == null || !dataUrl.startsWith("data:")) {
            return null;
        }
        
        // 瑙ｆ瀽 data URL 鏍煎紡: data:image/png;base64,xxxxx
        String mimeType = "image/png";
        int mimeEnd = dataUrl.indexOf(";");
        if (mimeEnd > 5) {
            mimeType = dataUrl.substring(5, mimeEnd);
        }
        
        return ImageData.builder()
                .url(dataUrl)
                .mimeType(mimeType)
                .dataType(DataType.DATA_URL)
                .build();
    }

    /**
     * 浠庡瓧鑺傛暟鎹垱寤?ImageData
     *
     * @param bytes    鍥剧墖瀛楄妭鏁版嵁
     * @param mimeType MIME 绫诲瀷
     * @return ImageData 瀹炰緥
     */
    public static ImageData fromBytes(byte[] bytes, String mimeType) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        
        return ImageData.builder()
                .bytes(bytes)
                .mimeType(mimeType != null ? mimeType : "image/png")
                .dataType(DataType.BYTES)
                .build();
    }

    /**
     * 鑾峰彇鍥剧墖瀛楄妭鏁版嵁
     * 濡傛灉鏄?data URL锛屼細瑙ｇ爜 base64
     *
     * @return 鍥剧墖瀛楄妭鏁版嵁
     */
    public byte[] getImageBytes() {
        if (dataType == DataType.BYTES) {
            return bytes;
        }
        
        if (dataType == DataType.DATA_URL && url != null) {
            // 瑙ｆ瀽 base64 data URL
            int base64Start = url.indexOf(",");
            if (base64Start > 0) {
                String base64Data = url.substring(base64Start + 1);
                return Base64.getDecoder().decode(base64Data);
            }
        }
        
        return null;
    }

    /**
     * 鍒ゆ柇鏄惁鏈夋湁鏁堟暟鎹?
     *
     * @return 鏄惁鏈夋晥
     */
    public boolean isValid() {
        return switch (dataType) {
            case BYTES -> bytes != null && bytes.length > 0;
            case URL, DATA_URL -> url != null && !url.isEmpty();
        };
    }

    /**
     * 鏍规嵁 MIME 绫诲瀷鑾峰彇鏂囦欢鎵╁睍鍚?
     *
     * @return 鏂囦欢鎵╁睍鍚嶏紙甯︾偣锛?
     */
    public String getFileExtension() {
        if (mimeType == null) {
            return ".png";
        }
        
        return switch (mimeType.toLowerCase()) {
            case "image/jpeg", "image/jpg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/gif" -> ".gif";
            case "image/webp" -> ".webp";
            case "image/svg+xml" -> ".svg";
            default -> ".png";
        };
    }
}
