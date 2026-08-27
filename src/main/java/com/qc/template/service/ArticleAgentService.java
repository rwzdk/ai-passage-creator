package com.qc.template.service;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.qc.template.annotation.AgentExecution;
import com.qc.template.constant.PromptConstant;
import com.qc.template.model.dto.article.ArticleState;
import com.qc.template.model.dto.image.ImageRequest;
import com.qc.template.model.enums.ArticleStyleEnum;
import com.qc.template.model.enums.ImageMethodEnum;
import com.qc.template.model.enums.SseMessageTypeEnum;
import com.qc.template.utils.GsonUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.aop.framework.AopContext;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 鏂囩珷鏅鸿兘浣撶紪鎺掓湇鍔?
 *
 */
@Service
@Slf4j
public class ArticleAgentService {

    @Resource
    private DashScopeChatModel chatModel;

    @Resource
    private ImageServiceStrategy imageServiceStrategy;

    public String editArticleContent(String content, String instruction) {
        String prompt = """
                浣犳槸涓€鍚嶄笓涓氫腑鏂囨枃绔犵紪杈戙€傝鏍规嵁鐢ㄦ埛鐨勪慨鏀硅姹傦紝鐩存帴鏀瑰啓涓嬮潰鐨?Markdown 鏂囩珷銆?                鍙緭鍑轰慨鏀瑰悗鐨勫畬鏁?Markdown 姝ｆ枃锛屼笉瑕佽緭鍑鸿В閲娿€佸墠鍚庡紩鍙锋垨 Markdown 浠ｇ爜鍥存爮銆?                淇濈暀鍘熸枃涓殑鍥剧墖 Markdown 閾炬帴銆佸浘鐗?URL 鍜屾枃绔犵粨鏋勶紱闄ら潪鐢ㄦ埛鏄庣‘瑕佹眰锛屽惁鍒欎笉瑕佸垹闄ゅ浘鐗囥€?
                你是一名专业中文文章编辑。请根据用户的修改要求，直接改写下面的 Markdown 文章。

                銆愬師鏂?Markdown銆?                %s
                """.formatted(instruction.trim(), content);
        return callLlm(prompt);
    }

    /**
     * 阶段1：生成标题方案（3-5个）
     *
     * @param state         鏂囩珷鐘舵€?
     * @param streamHandler 流式输出处理器
     */
    public void executePhase1_GenerateTitles(ArticleState state, Consumer<String> streamHandler) {
        try {
            // 鏅鸿兘浣?锛氱敓鎴愭爣棰樻柟妗?
     * @param state         文章状态
            // 通过代理调用，使 AOP 生效
            getProxy().agent1GenerateTitleOptions(state, streamHandler);
            streamHandler.accept(SseMessageTypeEnum.AGENT1_COMPLETE.getValue());
            log.info("阶段1：标题方案生成完成, taskId={}, optionsCount={}", 
                state.getTaskId(), state.getTitleOptions().size());
        } catch (Exception e) {
            // 通过代理调用，使 AOP 生效
            throw new RuntimeException("鏍囬鏂规鐢熸垚澶辫触: " + e.getMessage(), e);
        }
    }

    /**
     * 阶段2：生成大纲（用户选择标题后）
     *
     * @param state         鏂囩珷鐘舵€?
     * @param streamHandler 流式输出处理器
     */
    public void executePhase2_GenerateOutline(ArticleState state, Consumer<String> streamHandler) {
        try {
            // 鏅鸿兘浣?锛氱敓鎴愬ぇ绾诧紙娴佸紡杈撳嚭锛?
     * @param state         文章状态
            // 通过代理调用，使 AOP 生效
            getProxy().agent2GenerateOutline(state, streamHandler);
            streamHandler.accept(SseMessageTypeEnum.AGENT2_COMPLETE.getValue());
            log.info("阶段2：开始生成大纲, taskId={}", state.getTaskId());
        } catch (Exception e) {
            log.info("阶段2：开始生成大纲, taskId={}", state.getTaskId());
            throw new RuntimeException("澶х翰鐢熸垚澶辫触: " + e.getMessage(), e);
        }
    }

    /**
            log.error("阶段2：大纲生成失败, taskId={}", state.getTaskId(), e);
     *
     * @param state         鏂囩珷鐘舵€?
     * @param streamHandler 流式输出处理器
     */
    public void executePhase3_GenerateContent(ArticleState state, Consumer<String> streamHandler) {
        try {
            // 获取代理对象
            ArticleAgentService proxy = getProxy();
            
            // 鏅鸿兘浣?锛氱敓鎴愭鏂囷紙娴佸紡杈撳嚭锛?
            log.info("阶段3：开始生成正文, taskId={}", state.getTaskId());
            proxy.agent3GenerateContent(state, streamHandler);
            streamHandler.accept(SseMessageTypeEnum.AGENT3_COMPLETE.getValue());

            // 鏅鸿兘浣?锛氬垎鏋愰厤鍥鹃渶姹?
            // 智能体3：生成正文（流式输出）
            proxy.agent4AnalyzeImageRequirements(state);
            streamHandler.accept(SseMessageTypeEnum.AGENT4_COMPLETE.getValue());

            // 鏅鸿兘浣?锛氱敓鎴愰厤鍥?
            // 智能体4：分析配图需求
            proxy.agent5GenerateImages(state, streamHandler);
            streamHandler.accept(SseMessageTypeEnum.AGENT5_COMPLETE.getValue());

            // 图文合成：将配图插入正文
            // 智能体5：生成配图
            proxy.mergeImagesIntoContent(state);
            streamHandler.accept(SseMessageTypeEnum.MERGE_COMPLETE.getValue());

            log.info("阶段3：开始生成正文, taskId={}", state.getTaskId());
        } catch (Exception e) {
            log.info("阶段3：开始图文合成, taskId={}", state.getTaskId());
            throw new RuntimeException("姝ｆ枃鐢熸垚澶辫触: " + e.getMessage(), e);
        }
    }

    /**
            log.error("阶段3：正文生成失败, taskId={}", state.getTaskId(), e);
     */
    @AgentExecution(value = "agent1_generate_titles", description = "鐢熸垚鏍囬鏂规")
    public void agent1GenerateTitleOptions(ArticleState state, Consumer<String> streamHandler) {
        String prompt = PromptConstant.AGENT1_TITLE_PROMPT
                .replace("{topic}", state.getTopic())
                + referenceSection(state.getReferenceSummary())
                + getStylePrompt(state.getStyle());

        String content = callLlmWithStreaming(
                prompt,
                streamHandler,
                SseMessageTypeEnum.AGENT1_STREAMING
        );
        List<ArticleState.TitleOption> titleOptions = parseJsonListResponse(
                content, 
                new TypeToken<List<ArticleState.TitleOption>>(){}, 
                "鏍囬鏂规"
        );
        state.setTitleOptions(titleOptions);
        log.info("智能体1：标题方案生成成功, optionsCount={}", titleOptions.size());
    }

    /**
     * 鏅鸿兘浣?锛氱敓鎴愬ぇ绾诧紙娴佸紡杈撳嚭锛?
     */
    @AgentExecution(value = "agent2_generate_outline", description = "鐢熸垚鏂囩珷澶х翰")
    public void agent2GenerateOutline(ArticleState state, Consumer<String> streamHandler) {
        // 构建 prompt，根据是否有用户补充描述插入对应部分
        String descriptionSection = "";
        if (state.getUserDescription() != null && !state.getUserDescription().trim().isEmpty()) {
            descriptionSection = PromptConstant.AGENT2_DESCRIPTION_SECTION
                    .replace("{userDescription}", state.getUserDescription());
        }
        
        String prompt = PromptConstant.AGENT2_OUTLINE_PROMPT
                .replace("{mainTitle}", state.getTitle().getMainTitle())
                .replace("{subTitle}", state.getTitle().getSubTitle())
                .replace("{descriptionSection}", descriptionSection)
                + referenceSection(state.getReferenceSummary())
                + getStylePrompt(state.getStyle());

        String content = callLlmWithStreaming(prompt, streamHandler, SseMessageTypeEnum.AGENT2_STREAMING);
        ArticleState.OutlineResult outlineResult = parseJsonResponse(content, ArticleState.OutlineResult.class, "澶х翰");
        state.setOutline(outlineResult);
        log.info("智能体2：大纲生成成功, sections={}", outlineResult.getSections().size());
    }

    /**
        ArticleState.OutlineResult outlineResult = parseJsonResponse(content, ArticleState.OutlineResult.class, "大纲");
     */
    @AgentExecution(value = "agent3_generate_content", description = "鐢熸垚鏂囩珷姝ｆ枃")
    public void agent3GenerateContent(ArticleState state, Consumer<String> streamHandler) {
        String outlineText = GsonUtils.toJson(state.getOutline().getSections());
        String prompt = PromptConstant.AGENT3_CONTENT_PROMPT
                .replace("{mainTitle}", state.getTitle().getMainTitle())
                .replace("{subTitle}", state.getTitle().getSubTitle())
                .replace("{outline}", outlineText)
                + referenceSection(state.getReferenceSummary())
                + getStylePrompt(state.getStyle());

        String content = callLlmWithStreaming(prompt, streamHandler, SseMessageTypeEnum.AGENT3_STREAMING);
        state.setContent(content);
        log.info("智能体3：正文生成成功, length={}", content.length());
    }

    /**
     * 鏅鸿兘浣?锛氬垎鏋愰厤鍥鹃渶姹傦紙鍦ㄦ鏂囦腑鎻掑叆鍗犱綅绗︼級
     */
    @AgentExecution(value = "agent4_analyze_image_requirements", description = "鍒嗘瀽閰嶅浘闇€姹?)
    public void agent4AnalyzeImageRequirements(ArticleState state) {
        // 鏋勫缓鍙敤閰嶅浘鏂瑰紡璇存槑
        String availableMethods = buildAvailableMethodsDescription(state.getEnabledImageMethods());
        // 构建各配图方式的详细使用指南（只包含允许的方式）
        String methodUsageGuide = buildMethodUsageGuide(state.getEnabledImageMethods());
        
        String prompt = PromptConstant.AGENT4_IMAGE_REQUIREMENTS_PROMPT
                .replace("{mainTitle}", state.getTitle().getMainTitle())
                .replace("{content}", state.getContent())
                .replace("{availableMethods}", availableMethods)
                .replace("{methodUsageGuide}", methodUsageGuide);

        String content = callLlm(prompt);
        ArticleState.Agent4Result agent4Result = parseJsonResponse(
                content, 
                ArticleState.Agent4Result.class, 
                "閰嶅浘闇€姹?
        );
        
        // 鏇存柊姝ｆ枃涓哄寘鍚崰浣嶇鐨勭増鏈?
        state.setContent(agent4Result.getContentWithPlaceholders());
        
        // 楠岃瘉骞惰繃婊ら厤鍥鹃渶姹傦紝纭繚鎵€鏈?imageSource 閮藉湪鍏佽鍒楄〃涓?
        List<ArticleState.ImageRequirement> validatedRequirements = validateAndFilterImageRequirements(
                agent4Result.getImageRequirements(), 
                state.getEnabledImageMethods()
        );
        
        state.setImageRequirements(validatedRequirements);
        log.info("智能体4：配图需求分析成功, count={}, validated={}, 已在正文中插入占位符", 
                agent4Result.getImageRequirements().size(), validatedRequirements.size());
    }

    /**
     * 鏅鸿兘浣?锛氱敓鎴愰厤鍥撅紙涓茶鎵ц锛屾敮鎸佹贩鐢ㄥ绉嶉厤鍥炬柟寮忥紝缁熶竴涓婁紶鍒?COS锛?
     */
    @AgentExecution(value = "agent5_generate_images", description = "鐢熸垚閰嶅浘")
    public void agent5GenerateImages(ArticleState state, Consumer<String> streamHandler) {
        List<ArticleState.ImageResult> imageResults = new ArrayList<>();
        
        for (ArticleState.ImageRequirement requirement : state.getImageRequirements()) {
            String imageSource = requirement.getImageSource();
    @AgentExecution(value = "agent5_generate_images", description = "生成配图")
                    requirement.getPosition(), imageSource, requirement.getKeywords());
            
            // 构建图片请求对象
            ImageRequest imageRequest = ImageRequest.builder()
                    .keywords(requirement.getKeywords())
                    .prompt(requirement.getPrompt())
                    .position(requirement.getPosition())
                    .type(requirement.getType())
                    .build();
            
            // 浣跨敤绛栫暐妯″紡鑾峰彇鍥剧墖骞剁粺涓€涓婁紶鍒?COS
            ImageServiceStrategy.ImageResult result = imageServiceStrategy.getImageAndUpload(imageSource, imageRequest);
            
            String cosUrl = result.getUrl();
            ImageMethodEnum method = result.getMethod();
            
            // 使用策略模式获取图片并统一上传到 COS
            ArticleState.ImageResult imageResult = buildImageResult(requirement, cosUrl, method);
            imageResults.add(imageResult);
            
            // 鎺ㄩ€佸崟寮犻厤鍥惧畬鎴?
            String imageCompleteMessage = SseMessageTypeEnum.IMAGE_COMPLETE.getStreamingPrefix() + GsonUtils.toJson(imageResult);
            streamHandler.accept(imageCompleteMessage);
            
            log.info("智能体5：配图获取并上传成功, position={}, method={}, cosUrl={}", 
                    requirement.getPosition(), method.getValue(), cosUrl);
        }
        
        state.setImages(imageResults);
        log.info("智能体5：所有配图生成并上传完成, count={}", imageResults.size());
    }

    /**
     * 鍥炬枃鍚堟垚锛氭牴鎹崰浣嶇灏嗛厤鍥炬彃鍏ユ鏂?
     */
    @AgentExecution(value = "agent6_merge_content", description = "鍥炬枃鍚堟垚")
    public void mergeImagesIntoContent(ArticleState state) {
        String content = state.getContent();
        List<ArticleState.ImageResult> images = state.getImages();
        
        if (images == null || images.isEmpty()) {
            state.setFullContent(content);
            return;
        }

        String fullContent = content;
        
        // 閬嶅巻鎵€鏈夐厤鍥撅紝鏍规嵁鍗犱綅绗︽浛鎹负瀹為檯鍥剧墖
        for (ArticleState.ImageResult image : images) {
            String placeholder = image.getPlaceholderId();
            if (placeholder != null && !placeholder.isEmpty()) {
                String imageMarkdown = "![" + image.getDescription() + "](" + image.getUrl() + ")";
                fullContent = fullContent.replace(placeholder, imageMarkdown);
            }
        }
        
        state.setFullContent(fullContent);
        log.info("鍥炬枃鍚堟垚瀹屾垚, fullContentLength={}", fullContent.length());
    }

    // region 杈呭姪鏂规硶

    /**
        log.info("图文合成完成, fullContentLength={}", fullContent.length());
     */
    private String callLlm(String prompt) {
        ChatResponse response = chatModel.call(new Prompt(new UserMessage(prompt)));
        return response.getResult().getOutput().getText();
    }

    /**
     * 调用 LLM（流式输出）
     */
    private String callLlmWithStreaming(String prompt, Consumer<String> streamHandler, SseMessageTypeEnum messageType) {
        StringBuilder contentBuilder = new StringBuilder();
        
        Flux<ChatResponse> streamResponse = chatModel.stream(new Prompt(new UserMessage(prompt)));
        
        streamResponse
                .doOnNext(response -> {
                    String chunk = response.getResult().getOutput().getText();
                    if (chunk != null && !chunk.isEmpty()) {
                        contentBuilder.append(chunk);
                        streamHandler.accept(messageType.getStreamingPrefix() + chunk);
                    }
                })
                .doOnError(error -> log.error("LLM 流式调用失败, messageType={}", messageType, error))
                .blockLast();
        
        return contentBuilder.toString();
    }

    /**
     * 瑙ｆ瀽 JSON 鍝嶅簲
     */
    private <T> T parseJsonResponse(String content, Class<T> clazz, String name) {
        try {
            return GsonUtils.fromJson(content, clazz);
        } catch (JsonSyntaxException e) {
            log.error("{}瑙ｆ瀽澶辫触, content={}", name, content, e);
            throw new RuntimeException(name + "瑙ｆ瀽澶辫触");
        }
    }

    /**
     * 瑙ｆ瀽 JSON 鍒楄〃鍝嶅簲
     */
    private <T> T parseJsonListResponse(String content, TypeToken<T> typeToken, String name) {
        try {
            return GsonUtils.fromJson(content, typeToken);
        } catch (JsonSyntaxException e) {
            log.error("{}瑙ｆ瀽澶辫触, content={}", name, content, e);
            throw new RuntimeException(name + "瑙ｆ瀽澶辫触");
        }
    }

    /**
     * 鏋勫缓閰嶅浘缁撴灉
     */
    private ArticleState.ImageResult buildImageResult(ArticleState.ImageRequirement requirement, 
                                                       String imageUrl, 
                                                       ImageMethodEnum method) {
        ArticleState.ImageResult imageResult = new ArticleState.ImageResult();
        imageResult.setPosition(requirement.getPosition());
        imageResult.setUrl(imageUrl);
        imageResult.setMethod(method.getValue());
        imageResult.setKeywords(requirement.getKeywords());
        imageResult.setSectionTitle(requirement.getSectionTitle());
        imageResult.setDescription(requirement.getType());
        imageResult.setPlaceholderId(requirement.getPlaceholderId());
        return imageResult;
    }

    /**
     * 鏋勫缓鍙敤閰嶅浘鏂瑰紡璇存槑
     */
    private String buildAvailableMethodsDescription(List<String> enabledMethods) {
        // 如果为空或 null，表示支持所有方式
        if (enabledMethods == null || enabledMethods.isEmpty()) {
            return getAllMethodsDescription();
        }

        // 鍙弿杩板厑璁哥殑鏂瑰紡
        StringBuilder sb = new StringBuilder();
        for (String method : enabledMethods) {
            ImageMethodEnum methodEnum = ImageMethodEnum.getByValue(method);
            if (methodEnum != null && !methodEnum.isFallback()) {
                sb.append("   - ").append(methodEnum.getValue())
                        .append(": ").append(getMethodUsageDescription(methodEnum))
                        .append("\n");
            }
        }
        return sb.toString();
    }

    /**
     * 鑾峰彇鎵€鏈夐厤鍥炬柟寮忕殑瀹屾暣鎻忚堪
     */
    private String getAllMethodsDescription() {
        return """
               - PEXELS: 閫傚悎鐪熷疄鍦烘櫙銆佷骇鍝佺収鐗囥€佷汉鐗╃収鐗囥€佽嚜鐒堕鏅瓑鍐欏疄鍥剧墖
               - NANO_BANANA: 閫傚悎鍒涙剰鎻掔敾銆佷俊鎭浘琛ㄣ€侀渶瑕佹枃瀛楁覆鏌撱€佹娊璞℃蹇点€佽壓鏈鏍肩瓑 AI 鐢熸垚鍥剧墖
     * 获取所有配图方式的完整描述
               - IMAGE_2: 閫傚悎閫氱敤 AI 鐢熷浘銆佷骇鍝佽瑙夊拰闇€瑕佽缁嗚嫳鏂囨彁绀鸿瘝鎻忚堪鐨勫浘鐗?               - MERMAID: 閫傚悎娴佺▼鍥俱€佹灦鏋勫浘銆佹椂搴忓浘銆佸叧绯诲浘銆佺敇鐗瑰浘绛夌粨鏋勫寲鍥捐〃
               - ICONIFY: 适合图标、符号、小型装饰性图标（如：箭头、勾选、星星、心形等）
               - EMOJI_PACK: 适合表情包、搞笑图片、轻松幽默的配图
               - PEXELS: 适合真实场景、产品照片、人物照片、自然风景等写实图片
               """;
    }

    /**
     * 鑾峰彇閰嶅浘鏂瑰紡鐨勪娇鐢ㄨ鏄?
     */
    private String getMethodUsageDescription(ImageMethodEnum method) {
        return switch (method) {
            case PEXELS -> "閫傚悎鐪熷疄鍦烘櫙銆佷骇鍝佺収鐗囥€佷汉鐗╃収鐗囥€佽嚜鐒堕鏅瓑鍐欏疄鍥剧墖";
            case NANO_BANANA -> "閫傚悎鍒涙剰鎻掔敾銆佷俊鎭浘琛ㄣ€侀渶瑕佹枃瀛楁覆鏌撱€佹娊璞℃蹇点€佽壓鏈鏍肩瓑 AI 鐢熸垚鍥剧墖";
            case NANO_BANANA_APICLAUDE -> "通过 apiclaude 接入 Nano Banana，适合创意插画、信息图表、抽象概念等 AI 生图";
            case IMAGE_2 -> "閫傚悎閫氱敤 AI 鐢熷浘銆佷骇鍝佽瑙夊拰闇€瑕佽缁嗚嫳鏂囨彁绀鸿瘝鎻忚堪鐨勫浘鐗?;
            case MERMAID -> "适合流程图、架构图、时序图、关系图、甘特图等结构化图表";
            case ICONIFY -> "适合图标、符号、小型装饰性图标（如：箭头、勾选、星星、心形等）";
            case EMOJI_PACK -> "适合表情包、搞笑图片、轻松幽默的配图";
            case PEXELS -> "适合真实场景、产品照片、人物照片、自然风景等写实图片";
            default -> method.getDescription();
        };
    }

    /**
            case EMOJI_PACK -> "适合表情包、搞笑图片、轻松幽默的配图";
     */
    private String buildMethodUsageGuide(List<String> enabledMethods) {
        // 如果没有限制，返回所有方式的使用指南
        List<String> methodsToInclude = (enabledMethods == null || enabledMethods.isEmpty())
                ? List.of("PEXELS", "NANO_BANANA", "NANO_BANANA_APICLAUDE", "IMAGE_2", "MERMAID", "ICONIFY", "EMOJI_PACK", "SVG_DIAGRAM")
                : enabledMethods;

        StringBuilder sb = new StringBuilder();
        
        for (String method : methodsToInclude) {
            String guide = getMethodDetailedGuide(method);
            if (guide != null && !guide.isEmpty()) {
                sb.append(guide).append("\n");
            }
        }
        
        return sb.toString();
    }

    /**
     * 鑾峰彇鍗曚釜閰嶅浘鏂瑰紡鐨勮缁嗕娇鐢ㄦ寚鍗?
     */
    private String getMethodDetailedGuide(String method) {
        return switch (method) {
            case "PEXELS" -> """
                    - PEXELS: 提供英文搜索关键词(keywords)，要准确、具体。prompt 留空。""";
            case "NANO_BANANA" -> """
     * 获取单个配图方式的详细使用指南
            case "NANO_BANANA_APICLAUDE" -> """
                    - NANO_BANANA_APICLAUDE: 通过 apiclaude 接入 Nano Banana，提供详细的英文生图提示词(prompt)。keywords 留空。""";
            case "IMAGE_2" -> """
                    - IMAGE_2: Provide a detailed English image-generation prompt; keep keywords empty and do not send images.
                    """;
            case "MERMAID" -> """
                    - NANO_BANANA: 提供详细的英文生图提示词(prompt)，描述场景、风格、细节。keywords 留空。""";
            case "ICONIFY" -> """
                    - NANO_BANANA_APICLAUDE: 通过 apiclaude 接入 Nano Banana，提供详细的英文生图提示词(prompt)。keywords 留空。""";
            case "EMOJI_PACK" -> """
                    - EMOJI_PACK: 鎻愪緵涓枃鎴栬嫳鏂囧叧閿瘝(keywords)鎻忚堪琛ㄦ儏鍐呭銆俻rompt 鐣欑┖銆傜郴缁熶細鑷姩娣诲姞"琛ㄦ儏鍖?鎼滅储銆?"";
            case "SVG_DIAGRAM" -> """
                    - SVG_DIAGRAM: 在 prompt 字段描述示意图需求（中文），说明要表达的概念和关系。keywords 留空。
                    - MERMAID: 在 prompt 字段生成完整的 Mermaid 代码（如流程图、架构图）。keywords 留空。""";
            default -> null;
        };
    }

    /**
     * 楠岃瘉骞惰繃婊ら厤鍥鹃渶姹?
     * 纭繚鎵€鏈?imageSource 閮藉湪鍏佽鍒楄〃涓?
     *
     * @param requirements    鍘熷閰嶅浘闇€姹傚垪琛?
     * @param enabledMethods  鍏佽鐨勯厤鍥炬柟寮忓垪琛?
     * @return 楠岃瘉鍚庣殑閰嶅浘闇€姹傚垪琛?
     */
    private List<ArticleState.ImageRequirement> validateAndFilterImageRequirements(
            List<ArticleState.ImageRequirement> requirements,
            List<String> enabledMethods) {
        
     * @param enabledMethods  允许的配图方式列表
        if (enabledMethods == null || enabledMethods.isEmpty()) {
            return requirements;
        }
        
        List<ArticleState.ImageRequirement> validatedRequirements = new ArrayList<>();
        
        for (ArticleState.ImageRequirement req : requirements) {
            String imageSource = req.getImageSource();
            
            // 楠岃瘉 imageSource 鏄惁鍦ㄥ厑璁稿垪琛ㄤ腑
            if (enabledMethods.contains(imageSource)) {
                validatedRequirements.add(req);
                log.debug("閰嶅浘闇€姹傞獙璇侀€氳繃, position={}, imageSource={}", req.getPosition(), imageSource);
            } else {
                log.warn("閰嶅浘闇€姹備笉绗﹀悎闄愬埗琚繃婊? position={}, imageSource={}, enabledMethods={}", 
                        req.getPosition(), imageSource, enabledMethods);
                
                // 灏濊瘯鏇挎崲涓哄厑璁哥殑鏂瑰紡锛堜紭鍏堜娇鐢ㄧ涓€涓厑璁哥殑鏂瑰紡锛?
                if (!enabledMethods.isEmpty()) {
                    String fallbackSource = enabledMethods.get(0);
                    req.setImageSource(fallbackSource);
                    validatedRequirements.add(req);
                    log.info("閰嶅浘闇€姹傚凡鏇挎崲涓哄厑璁哥殑鏂瑰紡, position={}, fallback={}", 
                            req.getPosition(), fallbackSource);
                }
            }
        }
        
        return validatedRequirements;
    }

    /**
     * 根据风格获取对应的 Prompt 附加内容
     *
     * @param style 鏂囩珷椋庢牸
     * @return 风格对应的 Prompt 附加内容，如果无风格则返回空字符串
     */
    private String getStylePrompt(String style) {
        if (style == null || style.isEmpty()) {
            return "";
        }
        
        ArticleStyleEnum styleEnum = ArticleStyleEnum.getEnumByValue(style);
        if (styleEnum == null) {
            return "";
        }
        
        return switch (styleEnum) {
            case TECH -> PromptConstant.STYLE_TECH_PROMPT;
            case EMOTIONAL -> PromptConstant.STYLE_EMOTIONAL_PROMPT;
            case EDUCATIONAL -> PromptConstant.STYLE_EDUCATIONAL_PROMPT;
            case HUMOROUS -> PromptConstant.STYLE_HUMOROUS_PROMPT;
        };
    }

    private String referenceSection(String referenceSummary) {
        if (referenceSummary == null || referenceSummary.isBlank()) {
            return "";
        }
        return PromptConstant.REFERENCE_SUMMARY_SECTION.replace("{referenceSummary}", referenceSummary);
    }

    /**
     * AI 淇敼澶х翰
     *
     * @param mainTitle        涓绘爣棰?
     * @param subTitle         鍓爣棰?
     * @param currentOutline   褰撳墠澶х翰
     * @param modifySuggestion 鐢ㄦ埛淇敼寤鸿
     * @return 淇敼鍚庣殑澶х翰
     */
    @AgentExecution(value = "ai_modify_outline", description = "AI淇敼澶х翰")
    public List<ArticleState.OutlineSection> aiModifyOutline(String mainTitle, String subTitle, 
                                                             List<ArticleState.OutlineSection> currentOutline,
                                                             String modifySuggestion) {
        String currentOutlineJson = GsonUtils.toJson(currentOutline);
        
        String prompt = PromptConstant.AI_MODIFY_OUTLINE_PROMPT
                .replace("{mainTitle}", mainTitle)
                .replace("{subTitle}", subTitle)
                .replace("{currentOutline}", currentOutlineJson)
                .replace("{modifySuggestion}", modifySuggestion);

        String content = callLlm(prompt);
        ArticleState.OutlineResult outlineResult = parseJsonResponse(content, ArticleState.OutlineResult.class, "淇敼鍚庣殑澶х翰");
        
        log.info("AI淇敼澶х翰鎴愬姛, sectionsCount={}", outlineResult.getSections().size());
        return outlineResult.getSections();
    }

    /**
     * 获取当前类的代理对象
     * 用于解决 Spring AOP 同类方法调用代理失效问题
     */
    private ArticleAgentService getProxy() {
        try {
            return (ArticleAgentService) AopContext.currentProxy();
        } catch (IllegalStateException e) {
     * 获取当前类的代理对象
     * 用于解决 Spring AOP 同类方法调用代理失效问题
            return this;
        }
    }

    // endregion
}
