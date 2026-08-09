package com.yupi.template.service;

import com.google.gson.JsonObject;
import com.yupi.template.config.NanoBananaApiclaudeConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NanoBananaServiceTest {

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
}
