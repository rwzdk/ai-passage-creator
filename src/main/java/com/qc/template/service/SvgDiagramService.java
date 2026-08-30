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
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.xml.sax.InputSource;

import static com.qc.template.constant.ArticleConstant.PICSUM_URL_TEMPLATE;

/**
 * SVG 概念示意图生成服务
 * 使用 AI 生成 SVG 代码，适合概念示意、思维导图样式、关系展示等场景。
 *
 * @author <a href="https://codefather.cn">编程导航学习网</a>
 */
@Service
@Slf4j
public class SvgDiagramService implements ImageSearchService {

    private static final Pattern NUMBER_PATTERN = Pattern.compile("[-+]?(?:\\d+\\.?\\d*|\\.\\d+)(?:[eE][-+]?\\d+)?");
    private static final double MIN_CONTENT_WIDTH_RATIO = 0.25;
    private static final double MIN_CONTENT_HEIGHT_RATIO = 0.2;

    @Resource
    private SvgDiagramConfig svgDiagramConfig;

    @Resource
    private DashScopeChatModel chatModel;

    @Override
    public String searchImage(String keywords) {
        // 此方法已废弃，请使用 getImageData()
        // 返回 null，上传逻辑由 ImageServiceStrategy 统一处理
        return null;
    }

    @Override
    public ImageData getImageData(ImageRequest request) {
        String requirement = request.getEffectiveParam(true);
        return generateSvgDiagramData(requirement);
    }

    /**
     * 生成 SVG 概念示意图数据
     *
     * @param requirement 示意图需求描述
     * @return ImageData，包含 SVG 字节数据，生成失败返回 null
     */
    public ImageData generateSvgDiagramData(String requirement) {
        if (StrUtil.isBlank(requirement)) {
            log.warn("SVG diagram requirement is empty");
            return null;
        }

        try {
            // 1. 调用 LLM 生成 SVG 代码
            String svgCode = callLlmToGenerateSvg(requirement);

            if (StrUtil.isBlank(svgCode)) {
                log.error("LLM 未生成 SVG 代码");
                return null;
            }

            // 2. 校验并收紧 SVG 视口，避免模型生成大面积无效留白。
            svgCode = normalizeSvg(svgCode, svgDiagramConfig.getDefaultWidth(), svgDiagramConfig.getDefaultHeight());

            // 3. Convert the SVG to bytes.
            byte[] svgBytes = svgCode.getBytes(StandardCharsets.UTF_8);
            
            log.info("SVG 概念示意图生成成功: size={} bytes", svgBytes.length);
            return ImageData.fromBytes(svgBytes, "image/svg+xml");

        } catch (Exception e) {
            log.error("SVG 概念示意图生成异常: requirement={}", requirement, e);
            return null;
        }
    }

    /**
     * 调用 LLM 生成 SVG 代码
     */
    private String callLlmToGenerateSvg(String requirement) {
        String prompt = PromptConstant.SVG_DIAGRAM_GENERATION_PROMPT
                .replace("{requirement}", requirement);

        log.info("Generating SVG diagram with LLM");

        ChatResponse response = chatModel.call(new Prompt(new UserMessage(prompt)));
        String svgCode = response.getResult().getOutput().getText().trim();

        // 提取 SVG 代码（移除可能的 markdown 代码块标记）
        svgCode = extractSvgCode(svgCode);

        return svgCode;
    }

    /**
     * 提取 SVG 代码（去除 Markdown 代码块）
     */
    private String extractSvgCode(String text) {
        if (text == null) {
            return null;
        }

        // Remove markdown code fences.
        text = text.replace("```xml", "").replace("```svg", "").replace("```", "").trim();

        // 确保包含 XML 澹版槑
        if (!text.startsWith("<?xml")) {
            // Add an XML declaration when an SVG tag is present.
            if (text.contains("<svg")) {
                text = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + text;
            }
        }

        return text;
    }

    /**
     * 楠岃瘉 SVG 格式
     */
    static String normalizeSvg(String svgCode, Integer defaultWidth, Integer defaultHeight) {
        if (StrUtil.isBlank(svgCode)) {
            throw new IllegalArgumentException("SVG 内容为空");
        }

        int width = defaultWidth == null || defaultWidth < 1 ? 800 : defaultWidth;
        int height = defaultHeight == null || defaultHeight < 1 ? 600 : defaultHeight;
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);

            Document document = factory.newDocumentBuilder().parse(new InputSource(new StringReader(svgCode)));
            Element root = document.getDocumentElement();
            if (root == null || !"svg".equalsIgnoreCase(root.getLocalName() != null
                    ? root.getLocalName() : root.getTagName())) {
                throw new IllegalArgumentException("SVG 根节点无效");
            }
            rejectUnsafeSvg(document);

            double[] viewBox = parseViewBox(root.getAttribute("viewBox"), width, height);
            ContentBounds bounds = collectContentBounds(root, viewBox);
            if (!bounds.isValid()) {
                throw new IllegalArgumentException("SVG 没有可渲染内容");
            }

            double viewBoxWidth = viewBox[2];
            double viewBoxHeight = viewBox[3];
            if (!containsTransforms(root) && (bounds.width() < viewBoxWidth * MIN_CONTENT_WIDTH_RATIO
                    || bounds.height() < viewBoxHeight * MIN_CONTENT_HEIGHT_RATIO)) {
                double margin = Math.max(24, Math.min(viewBoxWidth, viewBoxHeight) * 0.06);
                double minX = Math.max(viewBox[0], bounds.minX - margin);
                double minY = Math.max(viewBox[1], bounds.minY - margin);
                double maxX = Math.min(viewBox[0] + viewBoxWidth, bounds.maxX + margin);
                double maxY = Math.min(viewBox[1] + viewBoxHeight, bounds.maxY + margin);
                if (maxX > minX && maxY > minY) {
                    viewBox = new double[]{minX, minY, maxX - minX, maxY - minY};
                }
            }

            root.setAttribute("viewBox", formatViewBox(viewBox));
            root.setAttribute("width", String.valueOf(width));
            root.setAttribute("height", String.valueOf(Math.max(1, Math.round(width * viewBox[3] / viewBox[2]))));
            root.setAttribute("preserveAspectRatio", "xMidYMid meet");
            return serializeSvg(document);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("SVG 结构解析失败", e);
        }
    }

    private static void rejectUnsafeSvg(Document document) {
        String[] forbiddenTags = {"script", "foreignObject", "iframe", "object", "embed"};
        for (String tag : forbiddenTags) {
            if (document.getElementsByTagNameNS("*", tag).getLength() > 0
                    || document.getElementsByTagName(tag).getLength() > 0) {
                throw new IllegalArgumentException("SVG 包含不允许的元素: " + tag);
            }
        }
        NodeList allElements = document.getElementsByTagName("*");
        for (int i = 0; i < allElements.getLength(); i++) {
            Node node = allElements.item(i);
            if (node instanceof Element element) {
                for (int j = 0; j < element.getAttributes().getLength(); j++) {
                    Node attribute = element.getAttributes().item(j);
                    String name = attribute.getNodeName().toLowerCase(Locale.ROOT);
                    String value = attribute.getNodeValue();
                    boolean externalReference = ("href".equals(name) || "xlink:href".equals(name))
                            && value != null && !value.trim().startsWith("#");
                    if (name.startsWith("on") || externalReference) {
                        throw new IllegalArgumentException("SVG 包含不允许的外部引用或事件属性");
                    }
                }
            }
        }
    }

    private static boolean containsTransforms(Element root) {
        if (root.hasAttribute("transform")) return true;
        NodeList elements = root.getElementsByTagName("*");
        for (int i = 0; i < elements.getLength(); i++) {
            if (elements.item(i) instanceof Element element && element.hasAttribute("transform")) {
                return true;
            }
        }
        return false;
    }

    private static double[] parseViewBox(String value, int width, int height) {
        Matcher matcher = NUMBER_PATTERN.matcher(value == null ? "" : value);
        double[] numbers = new double[4];
        int count = 0;
        while (matcher.find() && count < numbers.length) {
            numbers[count++] = Double.parseDouble(matcher.group());
        }
        if (count != 4 || numbers[2] <= 0 || numbers[3] <= 0) {
            return new double[]{0, 0, width, height};
        }
        return numbers;
    }

    private static ContentBounds collectContentBounds(Element root, double[] viewBox) {
        ContentBounds bounds = new ContentBounds();
        collectContentBounds(root, viewBox, bounds);
        return bounds;
    }

    private static void collectContentBounds(Element element, double[] viewBox, ContentBounds bounds) {
        String tag = element.getLocalName() != null ? element.getLocalName() : element.getTagName();
        if ("defs".equalsIgnoreCase(tag) || "style".equalsIgnoreCase(tag)
                || "title".equalsIgnoreCase(tag) || "desc".equalsIgnoreCase(tag)
                || "metadata".equalsIgnoreCase(tag)) {
            return;
        }
        if (!"svg".equalsIgnoreCase(tag) && !"g".equalsIgnoreCase(tag)
                && !isFullCanvasBackground(element, viewBox)) {
            addElementBounds(element, tag, bounds);
        }
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (children.item(i) instanceof Element child) {
                collectContentBounds(child, viewBox, bounds);
            }
        }
    }

    private static boolean isFullCanvasBackground(Element element, double[] viewBox) {
        String tag = element.getLocalName() != null ? element.getLocalName() : element.getTagName();
        if (!"rect".equalsIgnoreCase(tag)) return false;
        double width = number(element.getAttribute("width"), -1);
        double height = number(element.getAttribute("height"), -1);
        return width >= viewBox[2] * 0.95 && height >= viewBox[3] * 0.95;
    }

    private static void addElementBounds(Element element, String tag, ContentBounds bounds) {
        if ("rect".equalsIgnoreCase(tag) || "image".equalsIgnoreCase(tag)) {
            double x = number(element.getAttribute("x"), 0);
            double y = number(element.getAttribute("y"), 0);
            bounds.add(x, y, x + number(element.getAttribute("width"), 0), y + number(element.getAttribute("height"), 0));
        } else if ("circle".equalsIgnoreCase(tag)) {
            double cx = number(element.getAttribute("cx"), 0);
            double cy = number(element.getAttribute("cy"), 0);
            double r = number(element.getAttribute("r"), 0);
            bounds.add(cx - r, cy - r, cx + r, cy + r);
        } else if ("ellipse".equalsIgnoreCase(tag)) {
            double cx = number(element.getAttribute("cx"), 0);
            double cy = number(element.getAttribute("cy"), 0);
            double rx = number(element.getAttribute("rx"), 0);
            double ry = number(element.getAttribute("ry"), 0);
            bounds.add(cx - rx, cy - ry, cx + rx, cy + ry);
        } else if ("line".equalsIgnoreCase(tag)) {
            bounds.add(number(element.getAttribute("x1"), 0), number(element.getAttribute("y1"), 0),
                    number(element.getAttribute("x2"), 0), number(element.getAttribute("y2"), 0));
        } else if ("text".equalsIgnoreCase(tag)) {
            double x = number(element.getAttribute("x"), 0);
            double y = number(element.getAttribute("y"), 0);
            double fontSize = number(element.getAttribute("font-size"), 20);
            String[] lines = element.getTextContent().split("\\R", -1);
            int maxLength = 0;
            for (String line : lines) maxLength = Math.max(maxLength, line.trim().length());
            bounds.add(x, y - fontSize, x + Math.max(fontSize, maxLength * fontSize * 0.6), y + fontSize * 0.3);
        } else if ("polygon".equalsIgnoreCase(tag) || "polyline".equalsIgnoreCase(tag)) {
            addPointBounds(element.getAttribute("points"), bounds);
        } else if ("path".equalsIgnoreCase(tag)) {
            addPointBounds(element.getAttribute("d"), bounds);
        }
    }

    private static void addPointBounds(String value, ContentBounds bounds) {
        Matcher matcher = NUMBER_PATTERN.matcher(value == null ? "" : value);
        Double x = null;
        while (matcher.find()) {
            double number = Double.parseDouble(matcher.group());
            if (x == null) x = number;
            else {
                bounds.add(x, number, x, number);
                x = null;
            }
        }
    }

    private static double number(String value, double fallback) {
        Matcher matcher = NUMBER_PATTERN.matcher(value == null ? "" : value);
        return matcher.find() ? Double.parseDouble(matcher.group()) : fallback;
    }

    private static String formatViewBox(double[] viewBox) {
        return String.format(Locale.ROOT, "%.2f %.2f %.2f %.2f", viewBox[0], viewBox[1], viewBox[2], viewBox[3]);
    }

    private static String serializeSvg(Document document) throws Exception {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        transformerFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty(OutputKeys.INDENT, "no");
        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(document), new StreamResult(writer));
        return writer.toString();
    }

    private static final class ContentBounds {
        private double minX = Double.POSITIVE_INFINITY;
        private double minY = Double.POSITIVE_INFINITY;
        private double maxX = Double.NEGATIVE_INFINITY;
        private double maxY = Double.NEGATIVE_INFINITY;

        void add(double x1, double y1, double x2, double y2) {
            if (!Double.isFinite(x1) || !Double.isFinite(y1) || !Double.isFinite(x2) || !Double.isFinite(y2)) return;
            minX = Math.min(minX, Math.min(x1, x2));
            minY = Math.min(minY, Math.min(y1, y2));
            maxX = Math.max(maxX, Math.max(x1, x2));
            maxY = Math.max(maxY, Math.max(y1, y2));
        }

        boolean isValid() {
            return Double.isFinite(minX) && Double.isFinite(minY) && maxX > minX && maxY > minY;
        }

        double width() { return maxX - minX; }
        double height() { return maxY - minY; }
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
