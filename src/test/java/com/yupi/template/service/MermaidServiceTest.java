package com.yupi.template.service;

import com.yupi.template.config.MermaidConfig;
import com.yupi.template.model.dto.image.ImageRequest;
import com.yupi.template.model.enums.ImageMethodEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Mermaid 图表生成服务测试
 * 
 * 注意：此测试需要安装 mermaid-cli
 * 安装命令：npm install -g @mermaid-js/mermaid-cli
 *
 * @author <a href="https://codefather.cn">编程导航学习圈</a>
 */
@SpringBootTest
@ActiveProfiles("local")
class MermaidServiceTest {

    @Autowired
    private MermaidService mermaidService;

    @Autowired
    private MermaidConfig mermaidConfig;

    @Autowired
    private ImageServiceStrategy imageServiceStrategy;

    @BeforeEach
    void setUp() {
        assertNotNull(mermaidService, "MermaidService 未注入");
        assertNotNull(mermaidConfig, "MermaidConfig 未注入");
        assertNotNull(imageServiceStrategy, "ImageServiceStrategy 未注入");
    }

    @Test
    void testGetMethod() {
        // 验证服务类型
        assertEquals(ImageMethodEnum.MERMAID, mermaidService.getMethod());
        // 验证枚举元数据
        assertTrue(ImageMethodEnum.MERMAID.isAiGenerated(), "MERMAID 应该是 AI 生成方式");
    }

    @Test
    void testServiceRegistration() {
        // 验证服务已正确注册到策略选择器
        ImageSearchService service = imageServiceStrategy.getService(ImageMethodEnum.MERMAID);
        assertNotNull(service, "MermaidService 应该已注册到策略选择器");
        assertEquals(MermaidService.class, service.getClass());
    }

    @Test
    void testIsAvailable() {
        // 检查 mermaid-cli 是否可用
        boolean available = mermaidService.isAvailable();
        System.out.println("Mermaid CLI 可用性: " + available);
        
        if (!available) {
            System.out.println("跳过测试：未安装 mermaid-cli");
            System.out.println("安装命令：npm install -g @mermaid-js/mermaid-cli");
        }
    }

    /**
     * 测试生成流程图
     * 注意：需要安装 mermaid-cli
     */
    @Test
    void testGenerateFlowchart() {
        if (!mermaidService.isAvailable()) {
            System.out.println("跳过测试：未安装 mermaid-cli");
            return;
        }

        String mermaidCode = """
                flowchart TB
                    A[开始] --> B{判断条件}
                    B -->|是| C[执行操作]
                    B -->|否| D[跳过]
                    C --> E[结束]
                    D --> E
                """;

        System.out.println("开始生成流程图");
        System.out.println("Mermaid 代码:\n" + mermaidCode);

        String imageUrl = mermaidService.generateDiagram(mermaidCode);

        System.out.println("生成结果: " + (imageUrl != null ? "成功" : "失败"));
        if (imageUrl != null) {
            System.out.println("图片 URL: " + imageUrl);
        }

        assertNotNull(imageUrl, "流程图生成失败");
        assertTrue(imageUrl.contains("cos.") || imageUrl.contains("myqcloud.com"), 
                "图片应该已上传到 COS");
    }

    /**
     * 测试生成时序图
     */
    @Test
    void testGenerateSequenceDiagram() {
        if (!mermaidService.isAvailable()) {
            System.out.println("跳过测试：未安装 mermaid-cli");
            return;
        }

        String mermaidCode = """
                sequenceDiagram
                    participant 用户
                    participant 系统
                    participant 数据库
                    用户->>系统: 发送请求
                    系统->>数据库: 查询数据
                    数据库-->>系统: 返回结果
                    系统-->>用户: 响应数据
                """;

        System.out.println("开始生成时序图");
        String imageUrl = mermaidService.generateDiagram(mermaidCode);

        System.out.println("生成结果: " + (imageUrl != null ? "成功" : "失败"));
        assertNotNull(imageUrl, "时序图生成失败");
    }

    /**
     * 测试通过 ImageRequest 生成图表
     */
    @Test
    void testGenerateWithRequest() {
        if (!mermaidService.isAvailable()) {
            System.out.println("跳过测试：未安装 mermaid-cli");
            return;
        }

        String mermaidCode = """
                graph LR
                    A[前端] --> B[后端API]
                    B --> C[数据库]
                    B --> D[缓存]
                """;

        ImageRequest request = ImageRequest.builder()
                .prompt(mermaidCode)
                .position(2)
                .type("section")
                .build();

        System.out.println("开始通过 ImageRequest 生成图表");
        String imageUrl = mermaidService.getImage(request);

        System.out.println("生成结果: " + (imageUrl != null ? "成功" : "失败"));
        assertNotNull(imageUrl, "图表生成失败");
    }

    /**
     * 测试通过策略模式生成图表
     */
    @Test
    void testGenerateViaStrategy() {
        if (!mermaidService.isAvailable()) {
            System.out.println("跳过测试：未安装 mermaid-cli");
            return;
        }

        String mermaidCode = """
                pie title 用户分布
                    "活跃用户" : 45
                    "普通用户" : 35
                    "新用户" : 20
                """;

        ImageRequest request = ImageRequest.builder()
                .prompt(mermaidCode)
                .position(3)
                .type("section")
                .build();

        System.out.println("开始通过策略模式生成图表");

        ImageServiceStrategy.ImageResult result = imageServiceStrategy.getImage(
                ImageMethodEnum.MERMAID.getValue(),
                request
        );

        System.out.println("生成结果: " + (result.isSuccess() ? "成功" : "失败"));
        System.out.println("使用方法: " + result.getMethod().getDescription());

        assertTrue(result.isSuccess(), "图表生成失败");
        assertEquals(ImageMethodEnum.MERMAID, result.getMethod());
    }

    /**
     * 测试空代码处理
     */
    @Test
    void testEmptyCode() {
        String imageUrl = mermaidService.generateDiagram("");
        assertNull(imageUrl, "空代码应该返回 null");

        imageUrl = mermaidService.generateDiagram(null);
        assertNull(imageUrl, "null 代码应该返回 null");
    }

    /**
     * 测试降级图片
     */
    @Test
    void testGetFallbackImage() {
        String fallback = mermaidService.getFallbackImage(1);
        assertNotNull(fallback);
        assertTrue(fallback.contains("picsum.photos"));
    }
}
