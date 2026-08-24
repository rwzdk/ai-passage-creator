package com.qc.template.agent.parallel;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.qc.template.agent.context.StreamHandlerContext;
import com.qc.template.annotation.AgentExecution;
import com.qc.template.agent.tools.ImageGenerationTool;
import com.qc.template.model.dto.article.ArticleState;
import com.qc.template.model.enums.SseMessageTypeEnum;
import com.qc.template.utils.GsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

@Component
@Slf4j
@RequiredArgsConstructor
public class ParallelImageGenerator implements NodeAction {

    private final ImageGenerationTool imageGenerationTool;

    public static final String INPUT_IMAGE_REQUIREMENTS = "imageRequirements";
    public static final String INPUT_TASK_ID = "taskId";
    public static final String OUTPUT_IMAGES = "images";

    @Override
    @AgentExecution(value = "agent5_generate_images", description = "生成配图")
    public Map<String, Object> apply(OverAllState state) {
        List<ArticleState.ImageRequirement> imageRequirements = getImageRequirements(state);
        String taskId = state.value(INPUT_TASK_ID).map(Object::toString).orElse(null);
        Consumer<String> streamHandler = StreamHandlerContext.get(taskId);

        if (imageRequirements.isEmpty()) {
            return Map.of(OUTPUT_IMAGES, new ArrayList<>());
        }

        List<ArticleState.ImageResult> images = executeInPositionOrder(imageRequirements, streamHandler);
        StreamHandlerContext.send(taskId, SseMessageTypeEnum.AGENT5_COMPLETE.getStreamingPrefix()
                + GsonUtils.toJson(images));
        return Map.of(OUTPUT_IMAGES, images);
    }

    @SuppressWarnings("unchecked")
    private List<ArticleState.ImageRequirement> getImageRequirements(OverAllState state) {
        return state.value(INPUT_IMAGE_REQUIREMENTS)
                .map(value -> {
                    if (!(value instanceof List<?> list)) {
                        return new ArrayList<ArticleState.ImageRequirement>();
                    }
                    List<ArticleState.ImageRequirement> requirements = new ArrayList<>();
                    for (Object item : list) {
                        if (item instanceof ArticleState.ImageRequirement requirement) {
                            requirements.add(requirement);
                        } else if (item instanceof Map<?, ?>) {
                            requirements.add(GsonUtils.fromJson(GsonUtils.toJson(item), ArticleState.ImageRequirement.class));
                        }
                    }
                    return requirements;
                })
                .orElseGet(ArrayList::new);
    }

    private List<ArticleState.ImageResult> executeInPositionOrder(
            List<ArticleState.ImageRequirement> imageRequirements, Consumer<String> streamHandler) {
        List<ArticleState.ImageRequirement> sortedRequirements = imageRequirements.stream()
                .sorted((left, right) -> Integer.compare(positionOf(left), positionOf(right)))
                .toList();
        List<CompletableFuture<ArticleState.ImageResult>> futures = sortedRequirements.stream()
                .map(requirement -> CompletableFuture.supplyAsync(
                        () -> generateImage(requirement, streamHandler)))
                .toList();
        return futures.stream()
                .map(CompletableFuture::join)
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    private int positionOf(ArticleState.ImageRequirement requirement) {
        return requirement.getPosition() == null ? Integer.MAX_VALUE : requirement.getPosition();
    }

    private ArticleState.ImageResult generateImage(ArticleState.ImageRequirement requirement,
                                                   Consumer<String> streamHandler) {
        try {
            send(streamHandler, SseMessageTypeEnum.IMAGE_START, requirement);
            ImageGenerationTool.ImageGenerationResult result = imageGenerationTool.generateImageDirect(
                    requirement.getImageSource(), requirement.getKeywords(), requirement.getPrompt(),
                    requirement.getPosition(), requirement.getType(), requirement.getSectionTitle(),
                    requirement.getPlaceholderId());
            if (result.isSuccess()) {
                ArticleState.ImageResult image = toImageResult(result);
                send(streamHandler, SseMessageTypeEnum.IMAGE_COMPLETE, image);
                return image;
            }
            ArticleState.ImageResult failedImage = toFailedImage(requirement, result.getError());
            send(streamHandler, SseMessageTypeEnum.IMAGE_FAILED, failedImage);
            return failedImage;
        } catch (Exception exception) {
            ArticleState.ImageResult failedImage = toFailedImage(requirement, exception.getMessage());
            send(streamHandler, SseMessageTypeEnum.IMAGE_FAILED, failedImage);
            log.error("Image generation failed: imageSource={}, position={}",
                    requirement.getImageSource(), requirement.getPosition(), exception);
            return failedImage;
        }
    }

    private void send(Consumer<String> streamHandler, SseMessageTypeEnum type, Object payload) {
        if (streamHandler != null) {
            streamHandler.accept(type.getStreamingPrefix() + GsonUtils.toJson(payload));
        }
    }

    private ArticleState.ImageResult toImageResult(ImageGenerationTool.ImageGenerationResult source) {
        ArticleState.ImageResult result = new ArticleState.ImageResult();
        result.setPosition(source.getPosition());
        result.setUrl(source.getUrl());
        result.setMethod(source.getMethod());
        result.setKeywords(source.getKeywords());
        result.setSectionTitle(source.getSectionTitle());
        result.setDescription(source.getDescription());
        result.setPlaceholderId(source.getPlaceholderId());
        return result;
    }

    private ArticleState.ImageResult toFailedImage(ArticleState.ImageRequirement requirement, String error) {
        ArticleState.ImageResult result = new ArticleState.ImageResult();
        result.setPosition(requirement.getPosition());
        result.setMethod(requirement.getImageSource());
        result.setKeywords(requirement.getKeywords());
        result.setSectionTitle(requirement.getSectionTitle());
        result.setDescription(requirement.getType());
        result.setPlaceholderId(requirement.getPlaceholderId());
        result.setFailed(true);
        result.setError(error);
        return result;
    }
}
