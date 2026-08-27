package com.qc.template.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.qc.template.config.NanoBananaApiclaudeConfig;
import com.qc.template.model.dto.image.ImageData;
import com.qc.template.model.dto.image.ImageRequest;
import com.qc.template.model.enums.ImageMethodEnum;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

import static com.qc.template.constant.ArticleConstant.PICSUM_URL_TEMPLATE;

@Service
public class NanoBananaApiclaudeService implements ImageSearchService {
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final Logger log = LoggerFactory.getLogger(NanoBananaApiclaudeService.class);
    private static final int MAX_ERROR_RESPONSE_LENGTH = 1000;
    private final NanoBananaApiclaudeConfig config;
    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .callTimeout(150, TimeUnit.SECONDS)
            .build();

    public NanoBananaApiclaudeService(NanoBananaApiclaudeConfig config) {
        this.config = config;
    }

    @Override
    public String searchImage(String keywords) {
        ImageData data = generateImageData(keywords);
        return data == null ? null : data.getUrl();
    }

    @Override
    public ImageData getImageData(ImageRequest request) {
        String prompt = request.getEffectiveParam(true);
        if (request.getSourceImageUrl() != null && !request.getSourceImageUrl().isBlank()) {
            return editImageData(prompt, request.getSourceImageUrl());
        }
        return generateImageData(prompt);
    }

    /** 根据公网原图 URL 进行图改图。 */
    public ImageData editImageData(String prompt, String imageUrl) {
        if (prompt == null || prompt.isBlank() || imageUrl == null || imageUrl.isBlank() || !isAvailable()) return null;
        try {
            Request sourceRequest = new Request.Builder().url(imageUrl).get().build();
            try (Response sourceResponse = client.newCall(sourceRequest).execute()) {
                if (!sourceResponse.isSuccessful() || sourceResponse.body() == null) {
                    log.warn("Nano Banana Apiclaude 原图下载失败: status={}", sourceResponse.code());
                    return null;
                }
                byte[] sourceBytes = sourceResponse.body().bytes();
                String mime = sourceResponse.header("Content-Type", "image/png");
                int separator = mime.indexOf(';');
                if (separator > 0) mime = mime.substring(0, separator).trim();
                return requestEdit(prompt, sourceBytes, mime);
            }
        } catch (IOException | RuntimeException e) {
            log.warn("Nano Banana Apiclaude 图改图原图处理失败: message={}", e.getMessage());
            return null;
        }
    }

    private ImageData requestEdit(String prompt, byte[] sourceBytes, String mime) throws IOException {
        Request request = new Request.Builder()
                .url(config.getBaseUrl().replaceAll("/+$", "") + "/v1beta/models/" + config.getModel() + ":generateContent")
                .header("Authorization", "Bearer " + config.getApiKey())
                .header("x-goog-api-key", config.getApiKey())
                .post(RequestBody.create(buildRestEditPayload(prompt, sourceBytes, mime).toString(), JSON))
                .build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) return null;
            return parseImageResponse(response.body().string());
        }
    }

    public ImageData generateImageData(String prompt) {
        if (prompt == null || prompt.isBlank() || !isAvailable()) return null;
        Request request = new Request.Builder()
                .url(config.getBaseUrl().replaceAll("/+$", "") + "/v1beta/models/" + config.getModel() + ":generateContent")
                .header("Authorization", "Bearer " + config.getApiKey())
                .header("x-goog-api-key", config.getApiKey())
                .post(RequestBody.create(buildRestPayload(prompt).toString(), JSON))
                .build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                String responseBody = response.body() == null ? "" : response.body().string();
                log.warn("Nano Banana Apiclaude 请求失败: status={}, response={}",
                        response.code(), truncateResponseBody(responseBody));
                return null;
            }
            String responseBody = response.body().string();
            return parseImageResponse(responseBody);
        } catch (IOException | RuntimeException e) {
            log.warn("Nano Banana Apiclaude 请求异常: message={}", e.getMessage());
            return null;
        }
    }

    private ImageData parseImageResponse(String responseBody) {
        try {
            JsonObject root = JsonParser.parseString(responseBody).getAsJsonObject();
            JsonArray candidates = root.getAsJsonArray("candidates");
            if (candidates == null || candidates.isEmpty()) return null;
            JsonArray parts = candidates.get(0).getAsJsonObject().getAsJsonObject("content").getAsJsonArray("parts");
            for (var element : parts) {
                JsonObject part = element.getAsJsonObject();
                JsonObject inline = part.has("inlineData") ? part.getAsJsonObject("inlineData") : part.getAsJsonObject("inline_data");
                if (inline != null && inline.has("data")) {
                    byte[] bytes = Base64.getDecoder().decode(inline.get("data").getAsString());
                    String mime = inline.has("mimeType") ? inline.get("mimeType").getAsString()
                            : (inline.has("mime_type") ? inline.get("mime_type").getAsString() : "image/png");
                    return ImageData.fromBytes(bytes, mime);
                }
            }
            log.warn("Nano Banana Apiclaude 响应未包含图片: response={}",
                    truncateResponseBody(responseBody));
            return null;
        } catch (RuntimeException e) {
            log.warn("Nano Banana Apiclaude 请求异常: message={}", e.getMessage());
            return null;
        }
    }

    static String truncateResponseBody(String responseBody) {
        if (responseBody == null) return "";
        return responseBody.length() <= MAX_ERROR_RESPONSE_LENGTH
                ? responseBody
                : responseBody.substring(0, MAX_ERROR_RESPONSE_LENGTH);
    }

    JsonObject buildRestPayload(String prompt) {
        return buildRestPayload(prompt, null, null);
    }

    JsonObject buildRestEditPayload(String prompt, byte[] imageBytes, String mime) {
        return buildRestPayload(prompt, imageBytes, mime);
    }

    private JsonObject buildRestPayload(String prompt, byte[] imageBytes, String mime) {
        JsonObject imageConfig = new JsonObject();
        imageConfig.addProperty("aspectRatio", config.getAspectRatio());
        if (config.getModel().contains("gemini-3")) imageConfig.addProperty("imageSize", config.getImageSize());
        JsonObject generation = new JsonObject();
        JsonArray modalities = new JsonArray();
        modalities.add("TEXT");
        modalities.add("IMAGE");
        generation.add("responseModalities", modalities);
        generation.add("imageConfig", imageConfig);
        JsonObject part = new JsonObject();
        part.addProperty("text", prompt);
        JsonArray parts = new JsonArray();
        parts.add(part);
        if (imageBytes != null && imageBytes.length > 0) {
            JsonObject inlineData = new JsonObject();
            inlineData.addProperty("mimeType", mime == null || mime.isBlank() ? "image/png" : mime);
            inlineData.addProperty("data", Base64.getEncoder().encodeToString(imageBytes));
            JsonObject imagePart = new JsonObject();
            imagePart.add("inlineData", inlineData);
            parts.add(imagePart);
        }
        JsonObject content = new JsonObject();
        content.addProperty("role", "user");
        content.add("parts", parts);
        JsonArray contents = new JsonArray();
        contents.add(content);
        JsonObject payload = new JsonObject();
        payload.add("contents", contents);
        payload.add("generationConfig", generation);
        return payload;
    }

    @Override
    public ImageMethodEnum getMethod() {
        return ImageMethodEnum.NANO_BANANA_APICLAUDE;
    }

    @Override
    public String getFallbackImage(int position) {
        return String.format(PICSUM_URL_TEMPLATE, position);
    }

    @Override
    public boolean isAvailable() {
        return config.getApiKey() != null && !config.getApiKey().isBlank() && config.getBaseUrl() != null && !config.getBaseUrl().isBlank();
    }
}
