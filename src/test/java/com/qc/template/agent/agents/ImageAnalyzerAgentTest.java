package com.qc.template.agent.agents;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ImageAnalyzerAgentTest {

    private static final String JSON_OBJECT = "{\"contentWithPlaceholders\":\"正文\",\"imageRequirements\":[]}";

    @Test
    void normalizesJsonObject() {
        assertEquals(JSON_OBJECT, ImageAnalyzerAgent.normalizeJsonResponse(JSON_OBJECT));
    }

    @Test
    void unwrapsJsonStringContainingObject() {
        String wrapped = "\"{\\\"contentWithPlaceholders\\\":\\\"正文\\\",\\\"imageRequirements\\\":[]}\"";

        assertEquals(JSON_OBJECT, ImageAnalyzerAgent.normalizeJsonResponse(wrapped));
    }

    @Test
    void removesMarkdownJsonFence() {
        assertEquals(JSON_OBJECT, ImageAnalyzerAgent.normalizeJsonResponse("```json\n" + JSON_OBJECT + "\n```"));
    }
}
