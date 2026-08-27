package com.qc.template.service;

import cn.hutool.core.util.StrUtil;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.qc.template.config.SvgDiagramConfig;
import com.qc.template.constant.PromptConstant;
import com.qc.template.model.dto.image.ImageData;
import com.qc.template.model.dto.image.ImageRequest;
import com.qc.template.model.enums.ImageMethodEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.nio.charset.StandardCharsets;

import static com.qc.template.constant.ArticleConstant.PICSUM_URL_TEMPLATE;

/**
 * SVG 姒傚康绀烘剰鍥剧敓鎴愭湇鍔?
 * 浣跨敤 AI 鐢熸垚 SVG 浠ｇ爜锛岄€傚悎姒傚康绀烘剰銆佹€濈淮瀵煎浘鏍峰紡銆佸叧绯诲睍绀虹瓑鍦烘櫙
 *
 */
@Service
@Slf4j
public class SvgDiagramService implements ImageSearchService {

    @Resource
    private SvgDiagramConfig svgDiagramConfig;

    @Resource
    private DashScopeChatModel chatModel;

    @Override
    public String searchImage(String keywords) {
        // 姝ゆ柟娉曞凡搴熷純锛岃浣跨敤 getImageData()
        // 杩斿洖 null锛屼笂浼犻€昏緫鐢?ImageServiceStrategy 缁熶竴澶勭悊
        return null;
    }

    @Override
    public ImageData getImageData(ImageRequest request) {
        String requirement = request.getEffectiveParam(true);
        return generateSvgDiagramData(requirement);
    }

    /**
     * 鐢熸垚 SVG 姒傚康绀烘剰鍥炬暟鎹?
     *
     * @param requirement 绀烘剰鍥鹃渶姹傛弿杩?
     * @return ImageData 鍖呭惈 SVG 瀛楄妭鏁版嵁锛岀敓鎴愬け璐ヨ繑鍥?null
     */
    public ImageData generateSvgDiagramData(String requirement) {
        if (StrUtil.isBlank(requirement)) {
            log.warn("SVG 鍥捐〃闇€姹備负绌?);
            return null;
        }

        try {
            // 1. 璋冪敤 LLM 鐢熸垚 SVG 浠ｇ爜
            String svgCode = callLlmToGenerateSvg(requirement);

            if (StrUtil.isBlank(svgCode)) {
                log.error("LLM 鏈敓鎴?SVG 浠ｇ爜");
                return null;
            }

            // 2. 楠岃瘉 SVG 鏍煎紡
            if (!isValidSvg(svgCode)) {
                log.error("鐢熸垚鐨?SVG 浠ｇ爜鏍煎紡鏃犳晥");
                return null;
            }

            // 3. 杞崲涓哄瓧鑺傛暟鎹?
            byte[] svgBytes = svgCode.getBytes(StandardCharsets.UTF_8);
            
            log.info("SVG 姒傚康绀烘剰鍥剧敓鎴愭垚鍔? size={} bytes", svgBytes.length);
            return ImageData.fromBytes(svgBytes, "image/svg+xml");

        } catch (Exception e) {
            log.error("SVG 姒傚康绀烘剰鍥剧敓鎴愬紓甯? requirement={}", requirement, e);
            return null;
        }
    }

    /**
     * 璋冪敤 LLM 鐢熸垚 SVG 浠ｇ爜
     */
    private String callLlmToGenerateSvg(String requirement) {
        String prompt = PromptConstant.SVG_DIAGRAM_GENERATION_PROMPT
                .replace("{requirement}", requirement);

        log.info("寮€濮嬭皟鐢?LLM 鐢熸垚 SVG 姒傚康绀烘剰鍥?);

        ChatResponse response = chatModel.call(new Prompt(new UserMessage(prompt)));
        String svgCode = response.getResult().getOutput().getText().trim();

        // 鎻愬彇 SVG 浠ｇ爜锛堢Щ闄ゅ彲鑳界殑 markdown 浠ｇ爜鍧楁爣璁帮級
        svgCode = extractSvgCode(svgCode);

        return svgCode;
    }

    /**
     * 鎻愬彇 SVG 浠ｇ爜锛堝幓闄?markdown 浠ｇ爜鍧楋級
     */
    private String extractSvgCode(String text) {
        if (text == null) {
            return null;
        }

        // 鍘婚櫎 markdown 浠ｇ爜鍧楁爣璁?
        text = text.replace("```xml", "").replace("```svg", "").replace("```", "").trim();

        // 纭繚鍖呭惈 XML 澹版槑
        if (!text.startsWith("<?xml")) {
            // 濡傛灉娌℃湁 XML 澹版槑浣嗘湁 <svg 鏍囩锛屾坊鍔犲０鏄?
            if (text.contains("<svg")) {
                text = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + text;
            }
        }

        return text;
    }

    /**
     * 楠岃瘉 SVG 鏍煎紡
     */
    private boolean isValidSvg(String svgCode) {
        if (StrUtil.isBlank(svgCode)) {
            return false;
        }

        // 鍩烘湰楠岃瘉锛氬寘鍚?svg 鏍囩
        return svgCode.contains("<svg") && svgCode.contains("</svg>");
    }

    @Override
    public ImageMethodEnum getMethod() {
        return ImageMethodEnum.SVG_DIAGRAM;
    }

    @Override
    public String getFallbackImage(int position) {
        return String.format(PICSUM_URL_TEMPLATE, position);
    }
}
