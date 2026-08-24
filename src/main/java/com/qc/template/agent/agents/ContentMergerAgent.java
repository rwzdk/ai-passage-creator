package com.qc.template.agent.agents;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.qc.template.agent.tools.ImageGenerationTool;
import com.qc.template.annotation.AgentExecution;
import com.qc.template.agent.context.StreamHandlerContext;
import com.qc.template.model.dto.article.ArticleState;
import com.qc.template.model.enums.SseMessageTypeEnum;
import com.qc.template.utils.GsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 图文合成 Agent
 * 将配图插入到正文的相应位置
 *
 * @author YuanJian Studio
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ContentMergerAgent implements NodeAction {

    private static final Pattern IMAGE_PLACEHOLDER_PATTERN =
            Pattern.compile("\\{\\{(?:[A-Z_]+)?IMAGE_PLACEHOLDER_[^}]+}}" );

    public static final String INPUT_CONTENT = "content";
    public static final String INPUT_IMAGES = "images";
    public static final String INPUT_TASK_ID = "taskId";
    public static final String OUTPUT_FULL_CONTENT = "fullContent";

    @Override
    @AgentExecution(value = "agent6_merge_content", description = "图文合成")
    public Map<String, Object> apply(OverAllState state) throws Exception {
        String taskId = state.value(INPUT_TASK_ID).map(Object::toString).orElse(null);
        String content = state.value(INPUT_CONTENT)
                .map(Object::toString)
                .orElseThrow(() -> new IllegalArgumentException("缺少正文内容参数"));
        
        @SuppressWarnings("unchecked")
        List<ArticleState.ImageResult> images = state.value(INPUT_IMAGES)
                .map(v -> {
                    if (v instanceof List) {
                        List<?> list = (List<?>) v;
                        if (list.isEmpty()) {
                            return new ArrayList<ArticleState.ImageResult>();
                        }
                        // 检查列表元素类型
                        if (list.get(0) instanceof ArticleState.ImageResult) {
                            return (List<ArticleState.ImageResult>) v;
                        }
                        // 尝试转换
                        return convertToImageResults(list);
                    }
                    return new ArrayList<ArticleState.ImageResult>();
                })
                .orElse(new ArrayList<>());

        long coverCount = images.stream()
                .filter(image -> image.getPosition() != null && image.getPosition() == 1)
                .count();
        long bodyImageCount = images.size() - coverCount;
        Map<String, Object> mergeStats = new java.util.HashMap<>();
        mergeStats.put("totalImages", images.size());
        mergeStats.put("coverImages", coverCount);
        mergeStats.put("bodyImages", bodyImageCount);
        StreamHandlerContext.send(taskId, SseMessageTypeEnum.MERGE_START.getStreamingPrefix()
                + GsonUtils.toJson(mergeStats));
        
        log.info("ContentMergerAgent 开始执行: 正文长度={}, 图片数量={}", content.length(), images.size());
        
        String fullContent = mergeImagesIntoContent(content, images, taskId);
        
        log.info("ContentMergerAgent 执行完成: 完整内容长度={}", fullContent.length());
        StreamHandlerContext.send(taskId,
                SseMessageTypeEnum.MERGE_COMPLETE.getStreamingPrefix() + fullContent);
        
        return Map.of(OUTPUT_FULL_CONTENT, fullContent);
    }

    /**
     * 将配图插入正文（使用占位符替换）
     */
    private String mergeImagesIntoContent(String content, List<ArticleState.ImageResult> images, String taskId) {
        if (images == null || images.isEmpty()) {
            return IMAGE_PLACEHOLDER_PATTERN.matcher(content).replaceAll("");
        }

        String fullContent = content;
        Set<ArticleState.ImageResult> unmatchedImages = new HashSet<>(images);
        
        // 遍历所有配图，根据占位符替换为实际图片
        for (ArticleState.ImageResult image : images) {
            if (image.isFailed() || image.getUrl() == null || image.getUrl().isBlank()) {
                if (image.getPlaceholderId() != null) {
                    fullContent = fullContent.replace(image.getPlaceholderId(), "");
                }
                continue;
            }
            String placeholder = image.getPlaceholderId();
            log.info("处理图片: position={}, placeholderId={}, url={}", 
                    image.getPosition(), placeholder, image.getUrl());
            
            if (placeholder != null && !placeholder.isEmpty()) {
                String description = image.getDescription() != null ? image.getDescription() : "配图";
                String imageMarkdown = "![" + description + "](" + image.getUrl() + ")";
                
                if (fullContent.contains(placeholder)) {
                    fullContent = fullContent.replace(placeholder, imageMarkdown);
                    unmatchedImages.remove(image);
                    log.info("成功替换占位符: {} -> {}", placeholder, imageMarkdown.substring(0, Math.min(50, imageMarkdown.length())));
                } else {
                    log.warn("正文中未找到占位符: {}", placeholder);
                    sendSkippedImage(taskId, image, "正文中未找到对应占位符");
                }
            } else {
                log.warn("图片 position={} 的 placeholderId 为空", image.getPosition());
                sendSkippedImage(taskId, image, "缺少正文占位符");
            }
        }

        // LLM 可能遗漏或错填 placeholderId。只要正文仍有图片占位符，就用尚未合成的图片补齐，
        // 防止最终页面直接展示模板文本。
        List<ArticleState.ImageResult> remainingImages = unmatchedImages.stream()
                .filter(image -> !image.isFailed() && image.getUrl() != null && !image.getUrl().isBlank())
                .filter(image -> image.getPosition() == null || image.getPosition() != 1)
                .sorted(Comparator.comparing(
                        ArticleState.ImageResult::getPosition,
                        Comparator.nullsLast(Integer::compareTo)))
                .toList();
        for (ArticleState.ImageResult image : remainingImages) {
            Matcher matcher = IMAGE_PLACEHOLDER_PATTERN.matcher(fullContent);
            if (!matcher.find()) {
                log.warn("图片 position={} 未匹配正文占位符，已跳过插入", image.getPosition());
                sendSkippedImage(taskId, image, "正文中没有剩余占位符");
                break;
            }
            String placeholder = matcher.group();
            String description = image.getDescription() != null ? image.getDescription() : "配图";
            String imageMarkdown = "![" + description + "](" + image.getUrl() + ")";
            fullContent = fullContent.replace(placeholder, imageMarkdown);
            log.warn("图片 position={} 的 placeholderId 无法匹配，已补齐正文占位符: {}",
                    image.getPosition(), placeholder);
        }
        
        return fullContent;
    }

    private void sendSkippedImage(String taskId, ArticleState.ImageResult image, String reason) {
        Map<String, Object> data = new java.util.HashMap<>();
        data.put("position", image.getPosition());
        data.put("method", image.getMethod());
        data.put("reason", reason);
        StreamHandlerContext.send(taskId, SseMessageTypeEnum.IMAGE_SKIPPED.getStreamingPrefix()
                + GsonUtils.toJson(data));
    }

    /**
     * 转换列表为 ImageResult 列表
     */
    private List<ArticleState.ImageResult> convertToImageResults(List<?> list) {
        List<ArticleState.ImageResult> results = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof ArticleState.ImageResult) {
                results.add((ArticleState.ImageResult) item);
            } else if (item instanceof ImageGenerationTool.ImageGenerationResult) {
                // 从 ImageGenerationTool.ImageGenerationResult 转换
                ImageGenerationTool.ImageGenerationResult genResult = 
                        (ImageGenerationTool.ImageGenerationResult) item;
                if (genResult.isSuccess()) {
                    ArticleState.ImageResult imageResult = new ArticleState.ImageResult();
                    imageResult.setPosition(genResult.getPosition());
                    imageResult.setUrl(genResult.getUrl());
                    imageResult.setMethod(genResult.getMethod());
                    imageResult.setKeywords(genResult.getKeywords());
                    imageResult.setSectionTitle(genResult.getSectionTitle());
                    imageResult.setDescription(genResult.getDescription());
                    imageResult.setPlaceholderId(genResult.getPlaceholderId());
                    results.add(imageResult);
                }
            } else if (item instanceof Map) {
                // 从 Map 转换
                String json = GsonUtils.toJson(item);
                ArticleState.ImageResult imageResult = GsonUtils.fromJson(json, ArticleState.ImageResult.class);
                if (imageResult.getUrl() != null) {
                    results.add(imageResult);
                }
            }
        }
        return results;
    }
}
