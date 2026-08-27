package com.qc.template.service;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.ImageConfig;
import com.google.genai.types.Part;
import com.qc.template.config.NanoBananaConfig;
import com.qc.template.model.dto.image.ImageData;
import com.qc.template.model.dto.image.ImageRequest;
import com.qc.template.model.enums.ImageMethodEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;

import static com.qc.template.constant.ArticleConstant.PICSUM_URL_TEMPLATE;

/**
 * Nano Banana (Gemini 原生图片生成) 服务
 * 使用 Gemini 2.5 Flash Image 或 Gemini 3 Pro Image 模型生成图片
 */
@Service
@Slf4j
public class NanoBananaService implements ImageSearchService {

    @Resource
    private NanoBananaConfig nanoBananaConfig;

    @Override
    public String searchImage(String keywords) {
        return null;
    }

    @Override
    public ImageData getImageData(ImageRequest request) {
        return generateImageData(request.getEffectiveParam(true));
    }

    public ImageData generateImageData(String prompt) {
        try {
            Client genaiClient = Client.builder()
                    .apiKey(nanoBananaConfig.getApiKey())
                    .build();
            try {
                ImageConfig.Builder imageConfigBuilder = ImageConfig.builder()
                        .aspectRatio(nanoBananaConfig.getAspectRatio());
                String model = nanoBananaConfig.getModel();
                if (model != null && model.contains("gemini-3-pro")) {
                    imageConfigBuilder.imageSize(nanoBananaConfig.getImageSize());
                }
                GenerateContentConfig config = GenerateContentConfig.builder()
                        .responseModalities("TEXT", "IMAGE")
                        .imageConfig(imageConfigBuilder.build())
                        .build();
                GenerateContentResponse response = genaiClient.models.generateContent(
                        model != null ? model : "gemini-2.5-flash-image", prompt, config);
                if (response.parts() != null) {
                    for (Part part : response.parts()) {
                        if (part.inlineData().isPresent() && part.inlineData().get().data().isPresent()) {
                            var blob = part.inlineData().get();
                            return ImageData.fromBytes(blob.data().get(), blob.mimeType().orElse("image/png"));
                        }
                    }
                }
                return null;
            } finally {
                genaiClient.close();
            }
        } catch (Exception e) {
            log.error("Nano Banana 官方服务生成图片异常, prompt={}", prompt, e);
            return null;
        }
    }

    @Override
    public ImageMethodEnum getMethod() {
        return ImageMethodEnum.NANO_BANANA;
    }

    @Override
    public boolean isAvailable() {
        return nanoBananaConfig.getApiKey() != null && !nanoBananaConfig.getApiKey().isBlank();
    }

    @Override
    public String getFallbackImage(int position) {
        return String.format(PICSUM_URL_TEMPLATE, position);
    }
}
