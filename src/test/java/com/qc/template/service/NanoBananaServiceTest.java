package com.qc.template.service;

import com.google.gson.JsonObject;
import com.qc.template.config.NanoBananaApiclaudeConfig;
import com.qc.template.config.NanoBananaConfig;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NanoBananaServiceTest {

    @Test
    void officialServiceIsUnavailableWithoutApiKey() {
        NanoBananaService service = new NanoBananaService();
        ReflectionTestUtils.setField(service, "nanoBananaConfig", new NanoBananaConfig());

        assertEquals(false, service.isAvailable());
    }

    @Test
    void restPayloadUsesGeminiGenerateContentShape() {
        NanoBananaApiclaudeConfig config = new NanoBananaApiclaudeConfig();
        config.setApiKey("test-key");
        config.setModel("gemini-3-pro-image-preview");
        config.setAspectRatio("16:9");
        config.setImageSize("1K");
        NanoBananaApiclaudeService service = new NanoBananaApiclaudeService(config);

        JsonObject payload = service.buildRestPayload("A cinematic orange cat");

        assertEquals("A cinematic orange cat", payload.getAsJsonArray("contents")
                .get(0).getAsJsonObject().getAsJsonArray("parts")
                .get(0).getAsJsonObject().get("text").getAsString());
        assertEquals("IMAGE", payload.getAsJsonObject("generationConfig")
                .getAsJsonArray("responseModalities").get(1).getAsString());
        assertEquals("16:9", payload.getAsJsonObject("generationConfig")
                .getAsJsonObject("imageConfig").get("aspectRatio").getAsString());
        assertEquals("1K", payload.getAsJsonObject("generationConfig")
                .getAsJsonObject("imageConfig").get("imageSize").getAsString());
    }

    @Test
    void editPayloadIncludesInlineSourceImage() {
        NanoBananaApiclaudeConfig config = new NanoBananaApiclaudeConfig();
        config.setApiKey("test-key");
        config.setModel("gemini-3.1-flash-image-preview");
        config.setAspectRatio("1:1");
        config.setImageSize("1K");
        NanoBananaApiclaudeService service = new NanoBananaApiclaudeService(config);

        JsonObject payload = service.buildRestEditPayload("保持主体不变", new byte[]{1, 2, 3}, "image/png");
        JsonObject imagePart = payload.getAsJsonArray("contents").get(0).getAsJsonObject()
                .getAsJsonArray("parts").get(1).getAsJsonObject();

        assertEquals("image/png", imagePart.getAsJsonObject("inlineData").get("mimeType").getAsString());
        assertEquals("AQID", imagePart.getAsJsonObject("inlineData").get("data").getAsString());
    }

    @Test
    void apiclaudeServiceUsesExtendedImageGenerationTimeouts() {
        NanoBananaApiclaudeService service = new NanoBananaApiclaudeService(new NanoBananaApiclaudeConfig());

        okhttp3.OkHttpClient client = (okhttp3.OkHttpClient) ReflectionTestUtils.getField(service, "client");

        assertEquals(120_000, client.readTimeoutMillis());
        assertEquals(150_000, client.callTimeoutMillis());
        assertTrue(NanoBananaApiclaudeService.truncateResponseBody("x".repeat(1001)).length() <= 1000);
    }
}
