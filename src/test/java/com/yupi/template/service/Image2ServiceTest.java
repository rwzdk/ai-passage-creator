package com.yupi.template.service;

import com.google.gson.JsonObject;
import com.yupi.template.config.Image2Config;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Image2ServiceTest {

    @Test
    void generationPayloadUsesPromptOnly() {
        Image2Service service = new Image2Service(config());

        JsonObject payload = service.buildGenerationPayload("一只橘猫", "1024x1024", 1);

        assertEquals("gpt-image-2", payload.get("model").getAsString());
        assertEquals("一只橘猫", payload.get("prompt").getAsString());
        assertEquals("1024x1024", payload.get("size").getAsString());
        assertEquals(1, payload.get("n").getAsInt());
        assertFalse(payload.has("images"));
    }

    @Test
    void editPayloadUsesPublicImageAndOptionalMaskUrls() {
        Image2Service service = new Image2Service(config());

        JsonObject payload = service.buildEditPayload(
                "把背景改成海边日落",
                "https://example.com/source.png",
                "https://example.com/mask.png",
                "1024x1024",
                1);

        assertEquals("https://example.com/source.png",
                payload.getAsJsonArray("images").get(0).getAsJsonObject().get("image_url").getAsString());
        assertEquals("https://example.com/mask.png",
                payload.getAsJsonObject("mask").get("image_url").getAsString());
        assertTrue(payload.has("prompt"));
    }

    private Image2Config config() {
        Image2Config config = new Image2Config();
        config.setApiKey("test-key");
        config.setBaseUrl("https://apiclaude.cc");
        config.setModel("gpt-image-2");
        config.setSize("1024x1024");
        config.setN(1);
        return config;
    }
}
