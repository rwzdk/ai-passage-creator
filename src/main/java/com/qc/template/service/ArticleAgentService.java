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
                銆愮敤鎴蜂慨鏀硅姹傘€?                %s

                銆愬師鏂?Markdown銆?                %s
                """.formatted(instruction.trim(), content);
        return callLlm(prompt);
    }

    /**
     * 闃舵1锛氱敓鎴愭爣棰樻柟妗堬紙3-5涓級
     *
     * @param state         鏂囩珷鐘舵€?
     * @param streamHandler 娴佸紡杈撳嚭澶勭悊鍣?
     */
    public void executePhase1_GenerateTitles(ArticleState state, Consumer<String> streamHandler) {
        try {
            // 鏅鸿兘浣?锛氱敓鎴愭爣棰樻柟妗?
            log.info("闃舵1锛氬紑濮嬬敓鎴愭爣棰樻柟妗? taskId={}", state.getTaskId());
            // 閫氳繃浠ｇ悊璋冪敤锛屼娇 AOP 鐢熸晥
            getProxy().agent1GenerateTitleOptions(state, streamHandler);
            streamHandler.accept(SseMessageTypeEnum.AGENT1_COMPLETE.getValue());
            log.info("闃舵1锛氭爣棰樻柟妗堢敓鎴愬畬鎴? taskId={}, optionsCount={}", 
                state.getTaskId(), state.getTitleOptions().size());
        } catch (Exception e) {
            log.error("闃舵1锛氭爣棰樻柟妗堢敓鎴愬け璐? taskId={}", state.getTaskId(), e);
            throw new RuntimeException("鏍囬鏂规鐢熸垚澶辫触: " + e.getMessage(), e);
        }
    }

    /**
     * 闃舵2锛氱敓鎴愬ぇ绾诧紙鐢ㄦ埛閫夋嫨鏍囬鍚庯級
     *
     * @param state         鏂囩珷鐘舵€?
     * @param streamHandler 娴佸紡杈撳嚭澶勭悊鍣?
     */
    public void executePhase2_GenerateOutline(ArticleState state, Consumer<String> streamHandler) {
        try {
            // 鏅鸿兘浣?锛氱敓鎴愬ぇ绾诧紙娴佸紡杈撳嚭锛?
            log.info("闃舵2锛氬紑濮嬬敓鎴愬ぇ绾? taskId={}", state.getTaskId());
            // 閫氳繃浠ｇ悊璋冪敤锛屼娇 AOP 鐢熸晥
            getProxy().agent2GenerateOutline(state, streamHandler);
            streamHandler.accept(SseMessageTypeEnum.AGENT2_COMPLETE.getValue());
            log.info("闃舵2锛氬ぇ绾茬敓鎴愬畬鎴? taskId={}", state.getTaskId());
        } catch (Exception e) {
            log.error("闃舵2锛氬ぇ绾茬敓鎴愬け璐? taskId={}", state.getTaskId(), e);
            throw new RuntimeException("澶х翰鐢熸垚澶辫触: " + e.getMessage(), e);
        }
    }

    /**
     * 闃舵3锛氱敓鎴愭鏂?閰嶅浘锛堢敤鎴风‘璁ゅぇ绾插悗锛?
     *
     * @param state         鏂囩珷鐘舵€?
     * @param streamHandler 娴佸紡杈撳嚭澶勭悊鍣?
     */
    public void executePhase3_GenerateContent(ArticleState state, Consumer<String> streamHandler) {
        try {
            // 鑾峰彇浠ｇ悊瀵硅薄
            ArticleAgentService proxy = getProxy();
            
            // 鏅鸿兘浣?锛氱敓鎴愭鏂囷紙娴佸紡杈撳嚭锛?
            log.info("闃舵3锛氬紑濮嬬敓鎴愭鏂? taskId={}", state.getTaskId());
            proxy.agent3GenerateContent(state, streamHandler);
            streamHandler.accept(SseMessageTypeEnum.AGENT3_COMPLETE.getValue());

            // 鏅鸿兘浣?锛氬垎鏋愰厤鍥鹃渶姹?
            log.info("闃舵3锛氬紑濮嬪垎鏋愰厤鍥鹃渶姹? taskId={}", state.getTaskId());
            proxy.agent4AnalyzeImageRequirements(state);
            streamHandler.accept(SseMessageTypeEnum.AGENT4_COMPLETE.getValue());

            // 鏅鸿兘浣?锛氱敓鎴愰厤鍥?
            log.info("闃舵3锛氬紑濮嬬敓鎴愰厤鍥? taskId={}", state.getTaskId());
            proxy.agent5GenerateImages(state, streamHandler);
            streamHandler.accept(SseMessageTypeEnum.AGENT5_COMPLETE.getValue());

            // 鍥炬枃鍚堟垚锛氬皢閰嶅浘鎻掑叆姝ｆ枃
            log.info("闃舵3锛氬紑濮嬪浘鏂囧悎鎴? taskId={}", state.getTaskId());
            proxy.mergeImagesIntoContent(state);
            streamHandler.accept(SseMessageTypeEnum.MERGE_COMPLETE.getValue());

            log.info("闃舵3锛氭鏂囩敓鎴愬畬鎴? taskId={}", state.getTaskId());
        } catch (Exception e) {
            log.error("闃舵3锛氭鏂囩敓鎴愬け璐? taskId={}", state.getTaskId(), e);
            throw new RuntimeException("姝ｆ枃鐢熸垚澶辫触: " + e.getMessage(), e);
        }
    }

    /**
     * 鏅鸿兘浣?锛氱敓鎴愭爣棰樻柟妗堬紙3-5涓級
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
        log.info("鏅鸿兘浣?锛氭爣棰樻柟妗堢敓鎴愭垚鍔? optionsCount={}", titleOptions.size());
    }

    /**
     * 鏅鸿兘浣?锛氱敓鎴愬ぇ绾诧紙娴佸紡杈撳嚭锛?
     */
    @AgentExecution(value = "agent2_generate_outline", description = "鐢熸垚鏂囩珷澶х翰")
    public void agent2GenerateOutline(ArticleState state, Consumer<String> streamHandler) {
        // 鏋勫缓 prompt锛屾牴鎹槸鍚︽湁鐢ㄦ埛琛ュ厖鎻忚堪鎻掑叆瀵瑰簲閮ㄥ垎
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
        log.info("鏅鸿兘浣?锛氬ぇ绾茬敓鎴愭垚鍔? sections={}", outlineResult.getSections().size());
    }

    /**
     * 鏅鸿兘浣?锛氱敓鎴愭鏂囷紙娴佸紡杈撳嚭锛?
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
        log.info("鏅鸿兘浣?锛氭鏂囩敓鎴愭垚鍔? length={}", content.length());
    }

    /**
     * 鏅鸿兘浣?锛氬垎鏋愰厤鍥鹃渶姹傦紙鍦ㄦ鏂囦腑鎻掑叆鍗犱綅绗︼級
     */
    @AgentExecution(value = "agent4_analyze_image_requirements", description = "鍒嗘瀽閰嶅浘闇€姹?)
    public void agent4AnalyzeImageRequirements(ArticleState state) {
        // 鏋勫缓鍙敤閰嶅浘鏂瑰紡璇存槑
        String availableMethods = buildAvailableMethodsDescription(state.getEnabledImageMethods());
        // 鏋勫缓鍚勯厤鍥炬柟寮忕殑璇︾粏浣跨敤鎸囧崡锛堝彧鍖呭惈鍏佽鐨勬柟寮忥級
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
        log.info("鏅鸿兘浣?锛氶厤鍥鹃渶姹傚垎鏋愭垚鍔? count={}, validated={}, 宸插湪姝ｆ枃涓彃鍏ュ崰浣嶇", 
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
            log.info("鏅鸿兘浣?锛氬紑濮嬭幏鍙栭厤鍥? position={}, imageSource={}, keywords={}", 
                    requirement.getPosition(), imageSource, requirement.getKeywords());
            
            // 鏋勫缓鍥剧墖璇锋眰瀵硅薄
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
            
            // 鍒涘缓閰嶅浘缁撴灉锛圲RL 宸茬粡鏄?COS 鍦板潃锛?
            ArticleState.ImageResult imageResult = buildImageResult(requirement, cosUrl, method);
            imageResults.add(imageResult);
            
            // 鎺ㄩ€佸崟寮犻厤鍥惧畬鎴?
            String imageCompleteMessage = SseMessageTypeEnum.IMAGE_COMPLETE.getStreamingPrefix() + GsonUtils.toJson(imageResult);
            streamHandler.accept(imageCompleteMessage);
            
            log.info("鏅鸿兘浣?锛氶厤鍥捐幏鍙栧苟涓婁紶鎴愬姛, position={}, method={}, cosUrl={}", 
                    requirement.getPosition(), method.getValue(), cosUrl);
        }
        
        state.setImages(imageResults);
        log.info("鏅鸿兘浣?锛氭墍鏈夐厤鍥剧敓鎴愬苟涓婁紶瀹屾垚, count={}", imageResults.size());
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
     * 璋冪敤 LLM锛堥潪娴佸紡锛?
     */
    private String callLlm(String prompt) {
        ChatResponse response = chatModel.call(new Prompt(new UserMessage(prompt)));
        return response.getResult().getOutput().getText();
    }

    /**
     * 璋冪敤 LLM锛堟祦寮忚緭鍑猴級
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
                .doOnError(error -> log.error("LLM 娴佸紡璋冪敤澶辫触, messageType={}", messageType, error))
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
        // 濡傛灉涓虹┖鎴?null锛岃〃绀烘敮鎸佹墍鏈夋柟寮?
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
               - NANO_BANANA_APICLAUDE: 閫氳繃 apiclaude 鎺ュ叆 Nano Banana锛岄€傚悎鍒涙剰鎻掔敾銆佷俊鎭浘琛ㄣ€佹娊璞℃蹇电瓑 AI 鐢熷浘
               - IMAGE_2: 閫傚悎閫氱敤 AI 鐢熷浘銆佷骇鍝佽瑙夊拰闇€瑕佽缁嗚嫳鏂囨彁绀鸿瘝鎻忚堪鐨勫浘鐗?               - MERMAID: 閫傚悎娴佺▼鍥俱€佹灦鏋勫浘銆佹椂搴忓浘銆佸叧绯诲浘銆佺敇鐗瑰浘绛夌粨鏋勫寲鍥捐〃
               - ICONIFY: 閫傚悎鍥炬爣銆佺鍙枫€佸皬鍨嬭楗版€у浘鏍囷紙濡傦細绠ご銆佸嬀閫夈€佹槦鏄熴€佸績褰㈢瓑锛?
               - EMOJI_PACK: 閫傚悎琛ㄦ儏鍖呫€佹悶绗戝浘鐗囥€佽交鏉惧菇榛樼殑閰嶅浘
               - SVG_DIAGRAM: 閫傚悎姒傚康绀烘剰鍥俱€佹€濈淮瀵煎浘鏍峰紡銆侀€昏緫鍏崇郴灞曠ず锛堜笉娑夊強绮剧‘鏁版嵁锛?
               """;
    }

    /**
     * 鑾峰彇閰嶅浘鏂瑰紡鐨勪娇鐢ㄨ鏄?
     */
    private String getMethodUsageDescription(ImageMethodEnum method) {
        return switch (method) {
            case PEXELS -> "閫傚悎鐪熷疄鍦烘櫙銆佷骇鍝佺収鐗囥€佷汉鐗╃収鐗囥€佽嚜鐒堕鏅瓑鍐欏疄鍥剧墖";
            case NANO_BANANA -> "閫傚悎鍒涙剰鎻掔敾銆佷俊鎭浘琛ㄣ€侀渶瑕佹枃瀛楁覆鏌撱€佹娊璞℃蹇点€佽壓鏈鏍肩瓑 AI 鐢熸垚鍥剧墖";
            case NANO_BANANA_APICLAUDE -> "閫氳繃 apiclaude 鎺ュ叆 Nano Banana锛岄€傚悎鍒涙剰鎻掔敾銆佷俊鎭浘琛ㄣ€佹娊璞℃蹇电瓑 AI 鐢熷浘";
            case IMAGE_2 -> "閫傚悎閫氱敤 AI 鐢熷浘銆佷骇鍝佽瑙夊拰闇€瑕佽缁嗚嫳鏂囨彁绀鸿瘝鎻忚堪鐨勫浘鐗?;
            case MERMAID -> "閫傚悎娴佺▼鍥俱€佹灦鏋勫浘銆佹椂搴忓浘銆佸叧绯诲浘銆佺敇鐗瑰浘绛夌粨鏋勫寲鍥捐〃";
            case ICONIFY -> "閫傚悎鍥炬爣銆佺鍙枫€佸皬鍨嬭楗版€у浘鏍囷紙濡傦細绠ご銆佸嬀閫夈€佹槦鏄熴€佸績褰㈢瓑锛?;
            case EMOJI_PACK -> "閫傚悎琛ㄦ儏鍖呫€佹悶绗戝浘鐗囥€佽交鏉惧菇榛樼殑閰嶅浘";
            case SVG_DIAGRAM -> "閫傚悎姒傚康绀烘剰鍥俱€佹€濈淮瀵煎浘鏍峰紡銆侀€昏緫鍏崇郴灞曠ず锛堜笉娑夊強绮剧‘鏁版嵁锛?;
            default -> method.getDescription();
        };
    }

    /**
     * 鏋勫缓閰嶅浘鏂瑰紡鐨勮缁嗕娇鐢ㄦ寚鍗楋紙鍙寘鍚厑璁哥殑鏂瑰紡锛?
     */
    private String buildMethodUsageGuide(List<String> enabledMethods) {
        // 濡傛灉娌℃湁闄愬埗锛岃繑鍥炴墍鏈夋柟寮忕殑浣跨敤鎸囧崡
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
                    - PEXELS: 鎻愪緵鑻辨枃鎼滅储鍏抽敭璇?keywords)锛岃鍑嗙‘銆佸叿浣撱€俻rompt 鐣欑┖銆?"";
            case "NANO_BANANA" -> """
                    - NANO_BANANA: 鎻愪緵璇︾粏鐨勮嫳鏂囩敓鍥炬彁绀鸿瘝(prompt)锛屾弿杩板満鏅€侀鏍笺€佺粏鑺傘€俴eywords 鐣欑┖銆?"";
            case "NANO_BANANA_APICLAUDE" -> """
                    - NANO_BANANA_APICLAUDE: 閫氳繃 apiclaude 鎺ュ叆 Nano Banana锛屾彁渚涜缁嗙殑鑻辨枃鐢熷浘鎻愮ず璇?prompt)銆俴eywords 鐣欑┖銆?"";
            case "IMAGE_2" -> """
                    - IMAGE_2: Provide a detailed English image-generation prompt; keep keywords empty and do not send images.
                    """;
            case "MERMAID" -> """
                    - MERMAID: 鍦?prompt 瀛楁鐢熸垚瀹屾暣鐨?Mermaid 浠ｇ爜锛堝娴佺▼鍥俱€佹灦鏋勫浘锛夈€俴eywords 鐣欑┖銆?"";
            case "ICONIFY" -> """
                    - ICONIFY: 鎻愪緵鑻辨枃鍥炬爣鍏抽敭璇?keywords)锛屽锛歝heck銆乤rrow銆乻tar銆乭eart銆俻rompt 鐣欑┖銆?"";
            case "EMOJI_PACK" -> """
                    - EMOJI_PACK: 鎻愪緵涓枃鎴栬嫳鏂囧叧閿瘝(keywords)鎻忚堪琛ㄦ儏鍐呭銆俻rompt 鐣欑┖銆傜郴缁熶細鑷姩娣诲姞"琛ㄦ儏鍖?鎼滅储銆?"";
            case "SVG_DIAGRAM" -> """
                    - SVG_DIAGRAM: 鍦?prompt 瀛楁鎻忚堪绀烘剰鍥鹃渶姹傦紙涓枃锛夛紝璇存槑瑕佽〃杈剧殑姒傚康鍜屽叧绯汇€俴eywords 鐣欑┖銆?
                      绀轰緥锛氱粯鍒舵€濈淮瀵煎浘鏍峰紡鐨勫浘锛屼腑蹇冩槸"鑷緥"锛屽懆鍥?涓垎鏀細涔犳儻銆佺幆澧冦€佸弽棣堛€佺郴缁?"";
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
        
        // 濡傛灉娌℃湁闄愬埗锛岃繑鍥炴墍鏈夐渶姹?
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
     * 鏍规嵁椋庢牸鑾峰彇瀵瑰簲鐨?Prompt 闄勫姞鍐呭
     *
     * @param style 鏂囩珷椋庢牸
     * @return 椋庢牸瀵瑰簲鐨?Prompt 闄勫姞鍐呭锛屽鏋滄棤椋庢牸鍒欒繑鍥炵┖瀛楃涓?
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
     * 鑾峰彇褰撳墠绫荤殑浠ｇ悊瀵硅薄
     * 鐢ㄤ簬瑙ｅ喅 Spring AOP 鍚岀被鏂规硶璋冪敤浠ｇ悊澶辨晥闂
     */
    private ArticleAgentService getProxy() {
        try {
            return (ArticleAgentService) AopContext.currentProxy();
        } catch (IllegalStateException e) {
            // 濡傛灉鑾峰彇浠ｇ悊澶辫触锛岃繑鍥?this锛堥檷绾у鐞嗭級
            log.warn("鑾峰彇 AOP 浠ｇ悊瀵硅薄澶辫触锛屼娇鐢ㄥ師濮嬪璞? {}", e.getMessage());
            return this;
        }
    }

    // endregion
}
