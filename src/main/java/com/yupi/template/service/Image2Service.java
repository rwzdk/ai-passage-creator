package com.yupi.template.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.yupi.template.config.Image2Config;
import com.yupi.template.model.dto.image.ImageData;
import com.yupi.template.model.dto.image.ImageRequest;
import com.yupi.template.model.enums.ImageMethodEnum;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;

/**
 * GPT Image 2 图片生成服务，使用 apiclaude 的 OpenAI 兼容接口。
 */
@Service
public class Image2Service implements ImageSearchService {

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private final Image2Config config;
    private final OkHttpClient client = new OkHttpClient();

    public Image2Service(Image2Config config) {
        this.config = config;
    }

    @Override
    public String searchImage(String prompt) {
        ImageData imageData = generateImageData(prompt);
        return imageData == null ? null : imageData.getUrl();
    }

    @Override
    public ImageData getImageData(ImageRequest request) {
        return generateImageData(request.getEffectiveParam(true));
    }

    public ImageData generateImageData(String prompt) {
        JsonObject payload = buildGenerationPayload(prompt, config.getSize(), config.getN());
        return requestImage("/v1/images/generations", payload);
    }

    /**
     * 根据公网图片 URL 进行图改图，可选传入公网蒙版 URL。
     */
    public ImageData editImageData(String prompt, String imageUrl, String maskUrl) {
        JsonObject payload = buildEditPayload(prompt, imageUrl, maskUrl, config.getSize(), config.getN());
        return requestImage("/v1/images/edits", payload);
    }

    JsonObject buildGenerationPayload(String prompt, String size, int n) {
        JsonObject payload = new JsonObject();
        payload.addProperty("model", config.getModel());
        payload.addProperty("prompt", prompt);
        payload.addProperty("size", size);
        payload.addProperty("n", n);
        return payload;
    }

    JsonObject buildEditPayload(String prompt, String imageUrl, String maskUrl, String size, int n) {
        requirePublicImageUrl(imageUrl, "imageUrl");
        if (maskUrl != null && !maskUrl.isBlank()) {
            requirePublicImageUrl(maskUrl, "maskUrl");
        }

        JsonObject payload = buildGenerationPayload(prompt, size, n);
        JsonArray images = new JsonArray();
        JsonObject image = new JsonObject();
        image.addProperty("image_url", imageUrl);
        images.add(image);
        payload.add("images", images);

        if (maskUrl != null && !maskUrl.isBlank()) {
            JsonObject mask = new JsonObject();
            mask.addProperty("image_url", maskUrl);
            payload.add("mask", mask);
        }
        return payload;
    }

    private ImageData requestImage(String path, JsonObject payload) {
        if (!isAvailable()) {
            return null;
        }

        RequestBody body = RequestBody.create(payload.toString(), JSON);
        Request request = new Request.Builder()
                .url(normalizedBaseUrl() + path)
                .header("Authorization", "Bearer " + config.getApiKey())
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                return null;
            }
            return parseImageData(response.body().string());
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }

    private ImageData parseImageData(String responseBody) {
        JsonObject root = JsonParser.parseString(responseBody).getAsJsonObject();
        JsonArray data = root.getAsJsonArray("data");
        if (data == null || data.isEmpty()) {
            return null;
        }
        JsonObject item = data.get(0).getAsJsonObject();
        JsonElement url = item.get("url");
        if (url != null && !url.isJsonNull() && !url.getAsString().isBlank()) {
            return ImageData.fromUrl(url.getAsString());
        }
        JsonElement base64 = item.get("b64_json");
        if (base64 != null && !base64.isJsonNull() && !base64.getAsString().isBlank()) {
            return ImageData.fromDataUrl("data:image/png;base64," + base64.getAsString());
        }
        return null;
    }

    private void requirePublicImageUrl(String imageUrl, String fieldName) {
        if (imageUrl == null || imageUrl.isBlank()) {
            throw new IllegalArgumentException(fieldName + " 不能为空");
        }
        URI uri;
        try {
            uri = URI.create(imageUrl);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(fieldName + " 必须是公网图片直链", e);
        }
        if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                || uri.getHost() == null) {
            throw new IllegalArgumentException(fieldName + " 必须是公网图片直链");
        }
    }

    private String normalizedBaseUrl() {
        String baseUrl = config.getBaseUrl();
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    @Override
    public ImageMethodEnum getMethod() {
        return ImageMethodEnum.IMAGE_2;
    }

    @Override
    public String getFallbackImage(int position) {
        return String.format("https://picsum.photos/seed/%d/800/600", position);
    }

    @Override
    public boolean isAvailable() {
        return config.getApiKey() != null && !config.getApiKey().isBlank()
                && config.getModel() != null && !config.getModel().isBlank()
                && config.getBaseUrl() != null && !config.getBaseUrl().isBlank();
    }
}
