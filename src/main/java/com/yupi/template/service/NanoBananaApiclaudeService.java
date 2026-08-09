package com.yupi.template.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.yupi.template.config.NanoBananaApiclaudeConfig;
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
import java.util.Base64;

import static com.yupi.template.constant.ArticleConstant.PICSUM_URL_TEMPLATE;

@Service
public class NanoBananaApiclaudeService implements ImageSearchService {
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private final NanoBananaApiclaudeConfig config;
    private final OkHttpClient client = new OkHttpClient();

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
        return generateImageData(request.getEffectiveParam(true));
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
            if (!response.isSuccessful() || response.body() == null) return null;
            JsonObject root = JsonParser.parseString(response.body().string()).getAsJsonObject();
            JsonArray candidates = root.getAsJsonArray("candidates");
            if (candidates == null || candidates.isEmpty()) return null;
            JsonArray parts = candidates.get(0).getAsJsonObject().getAsJsonObject("content").getAsJsonArray("parts");
            for (var element : parts) {
                JsonObject part = element.getAsJsonObject();
                JsonObject inline = part.has("inlineData") ? part.getAsJsonObject("inlineData") : part.getAsJsonObject("inline_data");
                if (inline != null && inline.has("data")) {
                    byte[] bytes = Base64.getDecoder().decode(inline.get("data").getAsString());
                    String mime = inline.has("mimeType") ? inline.get("mimeType").getAsString() : "image/png";
                    return ImageData.fromBytes(bytes, mime);
                }
            }
            return null;
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }

    JsonObject buildRestPayload(String prompt) {
        JsonObject imageConfig = new JsonObject();
        imageConfig.addProperty("aspectRatio", config.getAspectRatio());
        if (config.getModel().contains("gemini-3-pro")) imageConfig.addProperty("imageSize", config.getImageSize());
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
