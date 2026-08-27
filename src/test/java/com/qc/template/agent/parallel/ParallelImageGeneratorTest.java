package com.qc.template.agent.parallel;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.qc.template.agent.context.StreamHandlerContext;
import com.qc.template.agent.tools.ImageGenerationTool;
import com.qc.template.model.dto.article.ArticleState;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ParallelImageGeneratorTest {

    private static final String TASK_ID = "ordered-image-progress";

    @AfterEach
    void cleanup() {
        StreamHandlerContext.clear(TASK_ID);
    }

    @Test
    void reportsMixedImageSourcesAsTheyComplete() throws Exception {
        ImageGenerationTool tool = mock(ImageGenerationTool.class);
        when(tool.generateImageDirect(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenAnswer(invocation -> result(invocation.getArgument(3)));

        List<String> events = Collections.synchronizedList(new ArrayList<>());
        StreamHandlerContext.set(TASK_ID, message -> {
            if (!message.startsWith("AGENT5_COMPLETE:")) {
                events.add(eventName(message));
            }
        });

        new ParallelImageGenerator(tool).apply(mockState(List.of(
                requirement(1, "PEXELS"),
                requirement(2, "MERMAID"),
                requirement(3, "PEXELS")
        )));

        assertEquals(6, events.size());
        assertTrue(events.containsAll(List.of(
                "START:1", "COMPLETE:1", "START:2", "COMPLETE:2", "START:3", "COMPLETE:3")));
    }

    private ImageGenerationTool.ImageGenerationResult result(Integer position) throws InterruptedException {
        Thread.sleep(position == 1 ? 60 : 10);
        ImageGenerationTool.ImageGenerationResult result = new ImageGenerationTool.ImageGenerationResult();
        result.setPosition(position);
        result.setUrl("https://example.com/" + position + ".png");
        result.setMethod("PEXELS");
        result.setSuccess(true);
        return result;
    }

    private OverAllState mockState(List<ArticleState.ImageRequirement> requirements) {
        OverAllState state = mock(OverAllState.class);
        when(state.value(ParallelImageGenerator.INPUT_IMAGE_REQUIREMENTS)).thenReturn(Optional.of(requirements));
        when(state.value(ParallelImageGenerator.INPUT_TASK_ID)).thenReturn(Optional.of(TASK_ID));
        return state;
    }

    private ArticleState.ImageRequirement requirement(int position, String source) {
        ArticleState.ImageRequirement requirement = new ArticleState.ImageRequirement();
        requirement.setPosition(position);
        requirement.setImageSource(source);
        requirement.setPlaceholderId("{{IMAGE_PLACEHOLDER_" + position + "}}");
        return requirement;
    }

    private String eventName(String message) {
        String[] parts = message.split(":", 2);
        com.google.gson.JsonObject payload = com.google.gson.JsonParser.parseString(parts[1]).getAsJsonObject();
        return ("IMAGE_START".equals(parts[0]) ? "START:" : "COMPLETE:") + payload.get("position").getAsInt();
    }
}
