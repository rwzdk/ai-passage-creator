package com.qc.template.agent.agents;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.qc.template.agent.context.StreamHandlerContext;
import com.qc.template.model.dto.article.ArticleState;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContentMergerAgentTest {

    private static final String TASK_ID = "merge-stats-task";

    @org.junit.jupiter.api.AfterEach
    void cleanup() {
        StreamHandlerContext.clear(TASK_ID);
    }

    @Test
    void replacesUnmappedImagePlaceholderUsingRemainingImageInPositionOrder() throws Exception {
        ArticleState.ImageResult cover = image(1, "", "https://example.com/cover.png");
        ArticleState.ImageResult section = image(2, "{{IMAGE_PLACEHOLDER_1}}", "https://example.com/section.png");
        ArticleState.ImageResult unmappedSection = image(3, "", "https://example.com/unmapped-section.png");

        OverAllState state = mockState(
                "intro\n\n{{IMAGE_PLACEHOLDER_1}}\n\nbody\n\n{{IMAGE_PLACEHOLDER_2}}",
                List.of(cover, section, unmappedSection)
        );

        Map<String, Object> result = new ContentMergerAgent().apply(state);
        String fullContent = (String) result.get(ContentMergerAgent.OUTPUT_FULL_CONTENT);

        assertTrue(fullContent.contains("https://example.com/section.png"));
        assertTrue(fullContent.contains("https://example.com/unmapped-section.png"));
        assertFalse(fullContent.contains("https://example.com/cover.png"));
        assertFalse(fullContent.contains("IMAGE_PLACEHOLDER"));
    }

    @Test
    void removesImagePlaceholdersWhenNoImageWasGenerated() throws Exception {
        OverAllState state = mockState(
                "intro\n\n{{IMAGE_PLACEHOLDER_1}}\n\nbody\n\n{{IMAGE_PLACEHOLDER_2}}",
                List.of()
        );

        Map<String, Object> result = new ContentMergerAgent().apply(state);
        String fullContent = (String) result.get(ContentMergerAgent.OUTPUT_FULL_CONTENT);

        assertFalse(fullContent.contains("IMAGE_PLACEHOLDER"));
        assertTrue(fullContent.contains("intro"));
        assertTrue(fullContent.contains("body"));
    }

    @Test
    void reportsCoverAndBodyImageCountsBeforeMerging() throws Exception {
        List<String> events = new ArrayList<>();
        StreamHandlerContext.set(TASK_ID, events::add);
        OverAllState state = mockState("content", List.of(
                image(1, "", "https://example.com/cover.png"),
                image(2, "{{IMAGE_PLACEHOLDER_1}}", "https://example.com/section.png")
        ));
        org.mockito.Mockito.when(state.value(ContentMergerAgent.INPUT_TASK_ID))
                .thenReturn(java.util.Optional.of(TASK_ID));

        new ContentMergerAgent().apply(state);

        assertTrue(events.get(0).contains("MERGE_START:"));
        assertTrue(events.get(0).contains("\"coverImages\":1"));
        assertTrue(events.get(0).contains("\"bodyImages\":1"));
    }

    private OverAllState mockState(String content, List<ArticleState.ImageResult> images) {
        OverAllState state = org.mockito.Mockito.mock(OverAllState.class);
        org.mockito.Mockito.when(state.value(ContentMergerAgent.INPUT_CONTENT))
                .thenReturn(java.util.Optional.of(content));
        org.mockito.Mockito.when(state.value(ContentMergerAgent.INPUT_IMAGES))
                .thenReturn(java.util.Optional.of(images));
        return state;
    }

    private ArticleState.ImageResult image(int position, String placeholderId, String url) {
        ArticleState.ImageResult image = new ArticleState.ImageResult();
        image.setPosition(position);
        image.setPlaceholderId(placeholderId);
        image.setUrl(url);
        image.setDescription("section");
        return image;
    }
}
