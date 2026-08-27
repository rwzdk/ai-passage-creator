package com.qc.template.service;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RuntimeUtil;
import cn.hutool.system.SystemUtil;
import com.qc.template.config.MermaidConfig;
import com.qc.template.model.dto.image.ImageData;
import com.qc.template.model.dto.image.ImageRequest;
import com.qc.template.model.enums.ImageMethodEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.io.File;

import static com.qc.template.constant.ArticleConstant.PICSUM_URL_TEMPLATE;

/**
 * Mermaid 娴佺▼鍥剧敓鎴愭湇鍔?
 * 浣跨敤 mermaid-cli 灏?Mermaid 浠ｇ爜杞崲涓哄浘鐗?
 *
 */
@Service
@Slf4j
public class MermaidService implements ImageSearchService {

    @Resource
    private MermaidConfig mermaidConfig;

    @Override
    public String searchImage(String keywords) {
        // 对于 Mermaid，keywords 就是 Mermaid 代码
        // 此方法已废弃，请使用 getImageData()
        ImageData imageData = generateDiagramData(keywords);
        // 杩斿洖 null锛屽洜涓轰笉鍐嶇洿鎺ヨ繑鍥?URL
        return null;
    }

    @Override
    public String getImage(ImageRequest request) {
        // 此方法已废弃，请使用 getImageData()
        // 杩斿洖 null锛屼笂浼犻€昏緫鐢?ImageServiceStrategy 缁熶竴澶勭悊
        return null;
    }

    @Override
    public ImageData getImageData(ImageRequest request) {
        // 优先使用 prompt（Mermaid 代码），否则使用 keywords
        String mermaidCode = request.getEffectiveParam(true);
        return generateDiagramData(mermaidCode);
    }

    /**
     * 鐢熸垚 Mermaid 鍥捐〃鏁版嵁
     *
     * @param mermaidCode Mermaid 浠ｇ爜
     * @return 鍥剧墖瀛楄妭鏁版嵁锛岀敓鎴愬け璐ヨ繑鍥?null
     */
    public ImageData generateDiagramData(String mermaidCode) {
        if (mermaidCode == null || mermaidCode.trim().isEmpty()) {
            log.warn("Mermaid 浠ｇ爜涓虹┖");
            return null;
        }

        File tempInputFile = null;
        File tempOutputFile = null;

        try {
            // 鍒涘缓涓存椂杈撳叆鏂囦欢
            tempInputFile = FileUtil.createTempFile("mermaid_input_", ".mmd", true);
            FileUtil.writeUtf8String(mermaidCode, tempInputFile);

            // 鍒涘缓涓存椂杈撳嚭鏂囦欢
            String outputExtension = "." + mermaidConfig.getOutputFormat();
            tempOutputFile = FileUtil.createTempFile("mermaid_output_", outputExtension, true);

            // 杞崲涓哄浘鐗?
            convertMermaidToImage(tempInputFile, tempOutputFile);

            // 妫€鏌ヨ緭鍑烘枃浠?
            if (!tempOutputFile.exists() || tempOutputFile.length() == 0) {
                log.error("Mermaid CLI 鎵ц澶辫触锛岃緭鍑烘枃浠朵笉瀛樺湪鎴栦负绌?);
                return null;
            }

            // 璇诲彇鍥剧墖瀛楄妭鏁版嵁
            byte[] imageBytes = FileUtil.readBytes(tempOutputFile);
            String mimeType = getMimeType(mermaidConfig.getOutputFormat());
            
            log.info("Mermaid 鍥捐〃鐢熸垚鎴愬姛, size={} bytes", imageBytes.length);
            return ImageData.fromBytes(imageBytes, mimeType);

        } catch (Exception e) {
            log.error("Mermaid 鍥捐〃鐢熸垚寮傚父", e);
            return null;
        } finally {
            // 清理临时文件
            if (tempInputFile != null) {
                FileUtil.del(tempInputFile);
            }
            if (tempOutputFile != null) {
                FileUtil.del(tempOutputFile);
            }
        }
    }

    /**
     * 鏍规嵁杈撳嚭鏍煎紡鑾峰彇 MIME 绫诲瀷
     */
    private String getMimeType(String format) {
        return switch (format.toLowerCase()) {
            case "png" -> "image/png";
            case "svg" -> "image/svg+xml";
            case "pdf" -> "application/pdf";
            default -> "image/png";
        };
    }

    /**
     * 璋冪敤 Mermaid CLI 杞崲涓哄浘鐗?
     */
    private void convertMermaidToImage(File inputFile, File outputFile) {
        try {
            // 鏍规嵁鎿嶄綔绯荤粺閫夋嫨鍛戒护
            String command = SystemUtil.getOsInfo().isWindows() ? "mmdc.cmd" : mermaidConfig.getCliCommand();

            // 鏋勫缓鍛戒护琛屽弬鏁?
            String cmdLine = String.format("%s -i %s -o %s -b %s",
                    command,
                    inputFile.getAbsolutePath(),
                    outputFile.getAbsolutePath(),
                    mermaidConfig.getBackgroundColor()
            );

            // 濡傛灉閰嶇疆浜嗗搴︼紝娣诲姞瀹藉害鍙傛暟
            if (mermaidConfig.getWidth() != null && mermaidConfig.getWidth() > 0) {
                cmdLine += " -w " + mermaidConfig.getWidth();
            }

            log.info("鎵ц Mermaid CLI 鍛戒护: {}", cmdLine);

            // 鎵ц鍛戒护锛堝甫瓒呮椂锛?
            String result = RuntimeUtil.execForStr(cmdLine);
            
            log.debug("Mermaid CLI 鎵ц缁撴灉: {}", result);

        } catch (Exception e) {
            log.error("鎵ц Mermaid CLI 澶辫触", e);
            throw new RuntimeException("Mermaid CLI 鎵ц澶辫触: " + e.getMessage(), e);
        }
    }

    @Override
    public ImageMethodEnum getMethod() {
        return ImageMethodEnum.MERMAID;
    }

    @Override
    public String getFallbackImage(int position) {
        return String.format(PICSUM_URL_TEMPLATE, position);
    }

    @Override
    public boolean isAvailable() {
        try {
            // 妫€鏌?mermaid-cli 鏄惁宸插畨瑁?
            String command = SystemUtil.getOsInfo().isWindows() ? "mmdc.cmd" : mermaidConfig.getCliCommand();
            String checkCmd = command + " --version";
            String version = RuntimeUtil.execForStr(checkCmd);
            log.info("Mermaid CLI 鐗堟湰: {}", version);
            return version != null && !version.isEmpty();
        } catch (Exception e) {
            log.warn("Mermaid CLI 涓嶅彲鐢? {}", e.getMessage());
            return false;
        }
    }
}
