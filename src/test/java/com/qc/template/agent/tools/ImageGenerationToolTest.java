package com.qc.template.agent.tools;

import com.qc.template.model.enums.ImageMethodEnum;
import com.qc.template.service.ImageServiceStrategy;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ImageGenerationToolTest {

    @Test
    void returnsFailureWhenImageStrategyCannotGenerateImage() {
        ImageServiceStrategy strategy = mock(ImageServiceStrategy.class);
        when(strategy.getImageAndUpload(eq("IMAGE_2"), any()))
                .thenReturn(ImageServiceStrategy.ImageResult.failure(
                        ImageMethodEnum.IMAGE_2, "IMAGE_2 图片生成失败"));

        ImageGenerationTool tool = new ImageGenerationTool();
        ReflectionTestUtils.setField(tool, "imageServiceStrategy", strategy);

        ImageGenerationTool.ImageGenerationResult result = tool.generateImageDirect(
                "IMAGE_2", null, "a red square", 1, "cover", "", "{{IMAGE_PLACEHOLDER_1}}"
        );

        assertFalse(result.isSuccess());
        assertEquals("IMAGE_2 图片生成失败", result.getError());
        assertEquals(ImageMethodEnum.IMAGE_2.getValue(), result.getMethod());
        assertEquals("{{IMAGE_PLACEHOLDER_1}}", result.getPlaceholderId());
    }
}
