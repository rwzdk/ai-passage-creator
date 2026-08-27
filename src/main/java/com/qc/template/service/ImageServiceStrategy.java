package com.qc.template.service;

import com.qc.template.model.dto.image.ImageData;
import com.qc.template.model.dto.image.ImageRequest;
import com.qc.template.model.enums.ImageMethodEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 鍥剧墖鏈嶅姟绛栫暐閫夋嫨鍣?
 * 鏍规嵁鍥剧墖鏉ユ簮绫诲瀷閫夋嫨瀵瑰簲鐨勫浘鐗囨湇鍔″疄鐜?
 * 
 * 璁捐璇存槑锛?
 * - 鑷姩娉ㄥ唽鎵€鏈?ImageSearchService 瀹炵幇
 * - 鏍规嵁 ImageMethodEnum 鐨勫厓鏁版嵁鑷姩閫夋嫨姝ｇ‘鐨勫弬鏁?
 * - 鏀寔鏈嶅姟鍙敤鎬ф鏌ュ拰鑷姩闄嶇骇
 * - 缁熶竴澶勭悊鍥剧墖涓婁紶鍒?COS
 *
 */
@Service
@Slf4j
public class ImageServiceStrategy {

    @Resource
    private List<ImageSearchService> imageSearchServices;

    @Resource
    private CosService cosService;

    /**
     * 图片服务映射：ImageMethodEnum -> ImageSearchService
     */
    private final Map<ImageMethodEnum, ImageSearchService> serviceMap = new EnumMap<>(ImageMethodEnum.class);

    @PostConstruct
    public void init() {
        // 灏嗘墍鏈?ImageSearchService 瀹炵幇娉ㄥ唽鍒版槧灏勮〃
        for (ImageSearchService service : imageSearchServices) {
            ImageMethodEnum method = service.getMethod();
            serviceMap.put(method, service);
            log.info("娉ㄥ唽鍥剧墖鏈嶅姟: {} -> {} (AI鐢熷浘: {}, 闄嶇骇: {})", 
                    method.getValue(), 
                    service.getClass().getSimpleName(),
                    method.isAiGenerated(),
                    method.isFallback());
        }
    }

    /**
     * 获取图片并上传到 COS（推荐方法）
     * 缁熶竴澶勭悊鎵€鏈夊浘鐗囨潵婧愮殑涓婁紶閫昏緫
     *
     * @param imageSource 图片来源
     * @param request     图片请求对象
     * @return 鍥剧墖鑾峰彇缁撴灉锛堝寘鍚?COS URL锛?
     */
    public ImageResult getImageAndUpload(String imageSource, ImageRequest request) {
        ImageMethodEnum method = resolveMethod(imageSource);
        ImageSearchService service = resolveService(method);
        ImageMethodEnum resolvedMethod = service != null ? service.getMethod() : method;
        
        if (service == null || !service.isAvailable()) {
            log.warn("鍥剧墖鏈嶅姟涓嶅彲鐢? {}, 灏濊瘯闄嶇骇", method);
            if (method.isAiGenerated()) {
                return ImageResult.failure(method, method.getValue() + " 鍥剧墖鏈嶅姟涓嶅彲鐢?);
            }
            return handleFallbackWithUpload(request.getPosition());
        }

        try {
            // 1. 鑾峰彇鍥剧墖鏁版嵁
            ImageData imageData = service.getImageData(request);
            
            if (imageData == null || !imageData.isValid()) {
                log.warn("鍥剧墖鏁版嵁鑾峰彇澶辫触, 浣跨敤闄嶇骇鏂规, method={}", method);
                if (resolvedMethod.isAiGenerated()) {
                    return ImageResult.failure(resolvedMethod, resolvedMethod.getValue() + " 鍥剧墖鐢熸垚澶辫触");
                }
                return handleFallbackWithUpload(request.getPosition());
            }
            
            // 2. 涓婁紶鍒?COS
            String folder = getFolderForMethod(resolvedMethod);
            String cosUrl = cosService.uploadImageData(imageData, folder);
            
            if (cosUrl != null && !cosUrl.isEmpty()) {
                log.info("鍥剧墖鑾峰彇骞朵笂浼犳垚鍔? method={}, cosUrl={}", method, cosUrl);
                return new ImageResult(cosUrl, resolvedMethod);
            } else {
                log.warn("鍥剧墖涓婁紶 COS 澶辫触, 浣跨敤闄嶇骇鏂规, method={}", method);
                if (resolvedMethod.isAiGenerated()) {
                    return ImageResult.failure(resolvedMethod, resolvedMethod.getValue() + " 鍥剧墖涓婁紶澶辫触");
                }
                return handleFallbackWithUpload(request.getPosition());
            }
        } catch (Exception e) {
            log.error("鑾峰彇鍥剧墖骞朵笂浼犲紓甯? method={}", method, e);
            if (resolvedMethod.isAiGenerated()) {
                return ImageResult.failure(resolvedMethod, resolvedMethod.getValue() + " 鍥剧墖鐢熸垚寮傚父");
            }
            return handleFallbackWithUpload(request.getPosition());
        }
    }

    /**
     * 鏍规嵁鍥剧墖璇锋眰鑾峰彇鍥剧墖
     *
     * @param imageSource 图片来源
     * @param request     图片请求对象
     * @return 鍥剧墖鑾峰彇缁撴灉
     * @deprecated 浣跨敤 getImageAndUpload() 鏇夸唬
     */
    @Deprecated
    public ImageResult getImage(String imageSource, ImageRequest request) {
        ImageMethodEnum method = resolveMethod(imageSource);
        ImageSearchService service = resolveService(method);
        ImageMethodEnum resolvedMethod = service != null ? service.getMethod() : method;
        
        if (service == null || !service.isAvailable()) {
            log.warn("鍥剧墖鏈嶅姟涓嶅彲鐢? {}, 灏濊瘯闄嶇骇", method);
            return handleFallback(request.getPosition());
        }

        String imageUrl = service.getImage(request);
        
        if (imageUrl != null && !imageUrl.isEmpty()) {
            return new ImageResult(imageUrl, resolvedMethod);
        } else {
            log.warn("鍥剧墖鑾峰彇澶辫触, 浣跨敤闄嶇骇鏂规, method={}", method);
            return handleFallback(request.getPosition());
        }
    }

    /**
     * 鏍规嵁鍥剧墖鏉ユ簮鑾峰彇瀵瑰簲鐨勫浘鐗囷紙鍏煎鏃ф帴鍙ｏ紝涓嶄笂浼犲埌 COS锛?
     *
     * @param imageSource 图片来源（PEXELS / NANO_BANANA 等）
     * @param keywords    鍏抽敭璇嶏紙鐢ㄤ簬鍥惧簱妫€绱級
     * @param prompt      鎻愮ず璇嶏紙鐢ㄤ簬 AI 鐢熷浘锛?
     * @return 鍥剧墖鑾峰彇缁撴灉
     * @deprecated 浣跨敤 getImageAndUpload() 鏇夸唬
     */
    @Deprecated
    public ImageResult getImage(String imageSource, String keywords, String prompt) {
        ImageRequest request = ImageRequest.builder()
                .keywords(keywords)
                .prompt(prompt)
                .build();
        return getImage(imageSource, request);
    }

    /**
     * 鏍规嵁鍥剧墖鏂规硶鑾峰彇 COS 鏂囦欢澶?
     */
    private String getFolderForMethod(ImageMethodEnum method) {
        return switch (method) {
            case PEXELS -> "pexels";
            case NANO_BANANA -> "nano-banana";
            case NANO_BANANA_APICLAUDE -> "nano-banana-apiclaude";
            case IMAGE_2 -> "image-2";
            case MERMAID -> "mermaid";
            case ICONIFY -> "iconify";
            case EMOJI_PACK -> "emoji-pack";
            case SVG_DIAGRAM -> "svg-diagram";
            case PICSUM -> "picsum";
        };
    }

    /**
     * 瑙ｆ瀽鍥剧墖鏉ユ簮锛屽鐞嗘湭鐭ュ€?
     */
    private ImageMethodEnum resolveMethod(String imageSource) {
        ImageMethodEnum method = ImageMethodEnum.getByValue(imageSource);
        if (method == null) {
            log.warn("鏈煡鐨勫浘鐗囨潵婧? {}, 榛樿浣跨敤 {}", imageSource, ImageMethodEnum.getDefaultSearchMethod());
            return ImageMethodEnum.getDefaultSearchMethod();
        }
        return method;
    }

    private ImageSearchService resolveService(ImageMethodEnum method) {
        ImageSearchService service = serviceMap.get(method);
        if (method != ImageMethodEnum.NANO_BANANA || (service != null && service.isAvailable())) {
            return service;
        }

        ImageSearchService apiclaudeService = serviceMap.get(ImageMethodEnum.NANO_BANANA_APICLAUDE);
        if (apiclaudeService != null && apiclaudeService.isAvailable()) {
            log.info("Nano Banana 瀹樻柟閰嶇疆涓嶅彲鐢紝鏀圭敤 Apiclaude 閰嶇疆");
            return apiclaudeService;
        }
        return service;
    }

    /**
     * 澶勭悊闄嶇骇閫昏緫
     */
    private ImageResult handleFallback(Integer position) {
        int pos = position != null ? position : 1;
        String fallbackUrl = getFallbackImage(pos);
        return new ImageResult(fallbackUrl, ImageMethodEnum.getFallbackMethod());
    }

    /**
     * 澶勭悊闄嶇骇閫昏緫
     */
    private ImageResult handleFallbackWithUpload(Integer position) {
        int pos = position != null ? position : 1;
        String fallbackUrl = getFallbackImage(pos);
        
        // 灏嗛檷绾у浘鐗囦篃涓婁紶鍒?COS
        ImageData fallbackData = ImageData.fromUrl(fallbackUrl);
        String cosUrl = cosService.uploadImageData(fallbackData, "fallback");
        
        // 濡傛灉涓婁紶澶辫触锛岀洿鎺ヤ娇鐢ㄥ師濮?URL
        String finalUrl = (cosUrl != null && !cosUrl.isEmpty()) ? cosUrl : fallbackUrl;
        return new ImageResult(finalUrl, ImageMethodEnum.getFallbackMethod());
    }

    /**
     * 鑾峰彇鎸囧畾鏂规硶鐨勫浘鐗囨湇鍔?
     *
     * @param method 鍥剧墖鏂规硶
     * @return 图片服务，未找到返回 null
     */
    public ImageSearchService getService(ImageMethodEnum method) {
        return serviceMap.get(method);
    }

    /**
     * 鑾峰彇闄嶇骇鍥剧墖
     *
     * @param position 浣嶇疆搴忓彿
     * @return 闄嶇骇鍥剧墖 URL
     */
    public String getFallbackImage(int position) {
        // 浼樺厛浣跨敤宸叉敞鍐屾湇鍔＄殑闄嶇骇鏂规
        ImageSearchService defaultService = serviceMap.get(ImageMethodEnum.getDefaultSearchMethod());
        if (defaultService != null) {
            return defaultService.getFallbackImage(position);
        }
        return String.format("https://picsum.photos/800/600?random=%d", position);
    }

    /**
     * 鑾峰彇鎵€鏈夊凡娉ㄥ唽鐨勫浘鐗囨湇鍔＄被鍨?
     */
    public List<ImageMethodEnum> getRegisteredMethods() {
        return List.copyOf(serviceMap.keySet());
    }

    /**
     * 鍥剧墖鑾峰彇缁撴灉
     */
    public static class ImageResult {
        private final String url;
        private final ImageMethodEnum method;
        private final String error;

        public ImageResult(String url, ImageMethodEnum method) {
            this(url, method, null);
        }

        private ImageResult(String url, ImageMethodEnum method, String error) {
            this.url = url;
            this.method = method;
            this.error = error;
        }

        public static ImageResult failure(ImageMethodEnum method, String error) {
            return new ImageResult(null, method, error);
        }

        public String getUrl() {
            return url;
        }

        public ImageMethodEnum getMethod() {
            return method;
        }

        public String getError() {
            return error;
        }

        public boolean isSuccess() {
            return url != null && !url.isEmpty();
        }
    }
}
