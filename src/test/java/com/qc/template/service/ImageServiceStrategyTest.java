package com.qc.template.service;

import com.qc.template.model.dto.image.ImageRequest;
import com.qc.template.model.enums.ImageMethodEnum;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ImageServiceStrategyTest {

    @Test
    void usesApiclaudeWhenOfficialNanoBananaIsNotConfigured() {
        ImageServiceStrategy strategy = strategyWith(
                new StubImageService(ImageMethodEnum.NANO_BANANA, false, "https://official.example/image.png"),
                new StubImageService(ImageMethodEnum.NANO_BANANA_APICLAUDE, true, "https://apiclaude.example/image.png")
        );

        ImageServiceStrategy.ImageResult result = strategy.getImage("NANO_BANANA", request());

        assertEquals("https://apiclaude.example/image.png", result.getUrl());
        assertEquals(ImageMethodEnum.NANO_BANANA_APICLAUDE, result.getMethod());
    }

    @Test
    void prefersOfficialNanoBananaWhenItIsConfigured() {
        ImageServiceStrategy strategy = strategyWith(
                new StubImageService(ImageMethodEnum.NANO_BANANA, true, "https://official.example/image.png"),
                new StubImageService(ImageMethodEnum.NANO_BANANA_APICLAUDE, true, "https://apiclaude.example/image.png")
        );

        ImageServiceStrategy.ImageResult result = strategy.getImage("NANO_BANANA", request());

        assertEquals("https://official.example/image.png", result.getUrl());
        assertEquals(ImageMethodEnum.NANO_BANANA, result.getMethod());
    }

    @Test
    void reportsAiGenerationFailureInsteadOfReturningFallbackImage() {
        ImageServiceStrategy strategy = strategyWith(
                new StubImageService(ImageMethodEnum.IMAGE_2, true, null)
        );

        ImageServiceStrategy.ImageResult result = strategy.getImageAndUpload("IMAGE_2", request());

        assertFalse(result.isSuccess());
        assertEquals(ImageMethodEnum.IMAGE_2, result.getMethod());
        assertEquals("IMAGE_2 图片生成失败", result.getError());
    }

    private ImageServiceStrategy strategyWith(ImageSearchService... services) {
        ImageServiceStrategy strategy = new ImageServiceStrategy();
        ReflectionTestUtils.setField(strategy, "imageSearchServices", List.of(services));
        strategy.init();
        return strategy;
    }

    private ImageRequest request() {
        return ImageRequest.builder().prompt("test prompt").build();
    }

    private record StubImageService(ImageMethodEnum method, boolean available, String imageUrl) implements ImageSearchService {
        @Override
        public String searchImage(String keywords) {
            return imageUrl;
        }

        @Override
        public ImageMethodEnum getMethod() {
            return method;
        }

        @Override
        public String getFallbackImage(int position) {
            return imageUrl;
        }

        @Override
        public boolean isAvailable() {
            return available;
        }
    }
}
