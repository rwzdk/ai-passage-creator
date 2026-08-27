package com.qc.template.service;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.http.HttpProtocol;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.region.Region;
import com.qc.template.config.CosConfig;
import com.qc.template.model.dto.image.ImageData;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

/**
 * 鑵捐浜?COS 鏈嶅姟
 *
 */
@Service
@Slf4j
public class CosService {

    @Resource
    private CosConfig cosConfig;

    private COSClient cosClient;

    private final OkHttpClient httpClient = new OkHttpClient();

    @PostConstruct
    public void init() {
        COSCredentials cred = new BasicCOSCredentials(cosConfig.getSecretId(), cosConfig.getSecretKey());
        Region region = new Region(cosConfig.getRegion());
        ClientConfig clientConfig = new ClientConfig(region);
        clientConfig.setHttpProtocol(HttpProtocol.https);
        cosClient = new COSClient(cred, clientConfig);
    }

    /**
     * 涓婁紶 ImageData 鍒?COS锛堢粺涓€鍏ュ彛锛?
     * 鏍规嵁鏁版嵁绫诲瀷鑷姩閫夋嫨涓婁紶鏂瑰紡
     *
     * @param imageData 鍥剧墖鏁版嵁瀵硅薄
     * @param folder    鏂囦欢澶?
     * @return COS 鍥剧墖 URL锛屼笂浼犲け璐ヨ繑鍥?null
     */
    public String uploadImageData(ImageData imageData, String folder) {
        if (imageData == null || !imageData.isValid()) {
            log.warn("ImageData 鏃犳晥锛屾棤娉曚笂浼?);
            return null;
        }

        try {
            return switch (imageData.getDataType()) {
                case BYTES -> uploadBytes(imageData.getBytes(), imageData.getMimeType(), folder);
                case URL -> uploadFromUrl(imageData.getUrl(), folder);
                case DATA_URL -> uploadFromDataUrl(imageData, folder);
            };
        } catch (Exception e) {
            log.error("涓婁紶 ImageData 鍒?COS 澶辫触, dataType={}", imageData.getDataType(), e);
            return null;
        }
    }

    /**
     * 涓婁紶瀛楄妭鏁版嵁鍒?COS
     *
     * @param bytes    鍥剧墖瀛楄妭鏁版嵁
     * @param mimeType MIME 绫诲瀷
     * @param folder   鏂囦欢澶?
     * @return COS 鍥剧墖 URL
     */
    public String uploadBytes(byte[] bytes, String mimeType, String folder) {
        if (bytes == null || bytes.length == 0) {
            log.warn("瀛楄妭鏁版嵁涓虹┖锛屾棤娉曚笂浼?);
            return null;
        }

        try {
            // 鐢熸垚鏂囦欢鍚?
            String extension = getExtensionFromMimeType(mimeType);
            String fileName = folder + "/" + UUID.randomUUID() + extension;

            // 涓婁紶鍒?COS
            try (InputStream inputStream = new ByteArrayInputStream(bytes)) {
                ObjectMetadata metadata = new ObjectMetadata();
                metadata.setContentLength(bytes.length);
                metadata.setContentType(mimeType != null ? mimeType : "image/png");

                PutObjectRequest putObjectRequest = new PutObjectRequest(
                        cosConfig.getBucket(), fileName, inputStream, metadata);

                cosClient.putObject(putObjectRequest);

                String cosUrl = buildCosUrl(fileName);
                log.info("瀛楄妭鏁版嵁涓婁紶鎴愬姛, size={} bytes, url={}", bytes.length, cosUrl);
                return cosUrl;
            }
        } catch (Exception e) {
            log.error("涓婁紶瀛楄妭鏁版嵁鍒?COS 澶辫触", e);
            return null;
        }
    }

    /**
     * 浠庡閮?URL 涓嬭浇骞朵笂浼犲埌 COS
     *
     * @param imageUrl 澶栭儴鍥剧墖 URL
     * @param folder   鏂囦欢澶?
     * @return COS 鍥剧墖 URL
     */
    public String uploadFromUrl(String imageUrl, String folder) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            log.warn("鍥剧墖 URL 涓虹┖锛屾棤娉曚笂浼?);
            return null;
        }

        try {
            // 涓嬭浇鍥剧墖
            Request request = new Request.Builder().url(imageUrl).build();
            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    log.error("涓嬭浇鍥剧墖澶辫触: {}, code={}", imageUrl, response.code());
                    return null;
                }

                byte[] imageBytes = response.body().bytes();
                String contentType = response.header("Content-Type", "image/jpeg");

                // 涓婁紶瀛楄妭鏁版嵁
                return uploadBytes(imageBytes, contentType, folder);
            }
        } catch (IOException e) {
            log.error("浠?URL 涓婁紶鍥剧墖鍒?COS 澶辫触: {}", imageUrl, e);
            return null;
        }
    }

    /**
     * 浠?base64 data URL 瑙ｇ爜骞朵笂浼犲埌 COS
     *
     * @param imageData ImageData 瀵硅薄锛堝寘鍚?data URL锛?
     * @param folder    鏂囦欢澶?
     * @return COS 鍥剧墖 URL
     */
    public String uploadFromDataUrl(ImageData imageData, String folder) {
        byte[] bytes = imageData.getImageBytes();
        if (bytes == null || bytes.length == 0) {
            log.warn("瑙ｇ爜 data URL 澶辫触锛屾棤娉曚笂浼?);
            return null;
        }

        return uploadBytes(bytes, imageData.getMimeType(), folder);
    }

    /**
     * 涓婁紶鍥剧墖鍒?COS锛堝吋瀹规棫鎺ュ彛锛?
     *
     * @param imageUrl 鍥剧墖 URL
     * @param folder   鏂囦欢澶?
     * @return COS 鍥剧墖 URL
     */
    public String uploadImage(String imageUrl, String folder) {
        String result = uploadFromUrl(imageUrl, folder);
        // 闄嶇骇锛氬鏋滀笂浼犲け璐ワ紝杩斿洖鍘熷 URL
        return result != null ? result : imageUrl;
    }

    /**
     * 鐩存帴浣跨敤鍥剧墖 URL锛堜笉涓婁紶鍒?COS锛?
     *
     * @param imageUrl 鍥剧墖 URL
     * @return 鍥剧墖 URL
     * @deprecated 浣跨敤 uploadImageData() 鏇夸唬
     */
    @Deprecated
    public String useDirectUrl(String imageUrl) {
        return imageUrl;
    }

    /**
     * 涓婁紶鏂囦欢鍒?COS
     *
     * @param file   鏂囦欢瀵硅薄
     * @param folder 鏂囦欢澶?
     * @return COS 鏂囦欢 URL
     */
    public String uploadFile(File file, String folder) {
        try {
            // 璇诲彇鏂囦欢
            byte[] fileBytes = java.nio.file.Files.readAllBytes(file.toPath());

            // 鐢熸垚鏂囦欢鍚?
            String extension = getFileExtension(file.getName());
            String fileName = folder + "/" + UUID.randomUUID() + extension;

            // 涓婁紶鍒?COS
            try (InputStream inputStream = new ByteArrayInputStream(fileBytes)) {
                ObjectMetadata metadata = new ObjectMetadata();
                metadata.setContentLength(fileBytes.length);
                metadata.setContentType(getContentType(extension));

                PutObjectRequest putObjectRequest = new PutObjectRequest(
                        cosConfig.getBucket(), fileName, inputStream, metadata);

                cosClient.putObject(putObjectRequest);

                // 杩斿洖璁块棶 URL
                return String.format("https://%s.cos.%s.myqcloud.com/%s",
                        cosConfig.getBucket(), cosConfig.getRegion(), fileName);
            }
        } catch (IOException e) {
            log.error("涓婁紶鏂囦欢鍒?COS 澶辫触: {}", file.getName(), e);
            return null;
        }
    }

    /**
     * 鑾峰彇鏂囦欢鎵╁睍鍚?
     */
    private String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        return lastDot > 0 ? fileName.substring(lastDot) : ".svg";
    }

    /**
     * 鏍规嵁鎵╁睍鍚嶈幏鍙?Content-Type
     */
    private String getContentType(String extension) {
        return switch (extension.toLowerCase()) {
            case ".svg" -> "image/svg+xml";
            case ".png" -> "image/png";
            case ".jpg", ".jpeg" -> "image/jpeg";
            case ".gif" -> "image/gif";
            case ".webp" -> "image/webp";
            case ".pdf" -> "application/pdf";
            default -> "application/octet-stream";
        };
    }

    /**
     * 鏍规嵁 MIME 绫诲瀷鑾峰彇鏂囦欢鎵╁睍鍚?
     */
    private String getExtensionFromMimeType(String mimeType) {
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

    /**
     * 鏋勫缓 COS 璁块棶 URL
     */
    private String buildCosUrl(String fileName) {
        return String.format("https://%s.cos.%s.myqcloud.com/%s",
                cosConfig.getBucket(), cosConfig.getRegion(), fileName);
    }
}
