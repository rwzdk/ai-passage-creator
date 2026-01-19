package com.yupi.template.service;

import com.yupi.template.model.dto.image.ImageRequest;
import com.yupi.template.model.enums.ImageMethodEnum;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 表情包检索服务测试
 *
 * @author <a href="https://codefather.cn">编程导航学习圈</a>
 */
@SpringBootTest
@ActiveProfiles("local")
class EmojiPackServiceTest {

    @Autowired
    private ImageServiceStrategy imageServiceStrategy;

    /**
     * 测试通过策略模式获取表情包
     */
    @Test
    void testGetEmojiViaStrategy() {
        ImageRequest request = ImageRequest.builder()
                .keywords("开心")
                .position(1)
                .type("emoji")
                .build();

        System.out.println("通过策略模式获取表情包");

        ImageServiceStrategy.ImageResult result = imageServiceStrategy.getImage(
                ImageMethodEnum.EMOJI_PACK.getValue(),
                request
        );

        System.out.println("获取结果: " + (result.isSuccess() ? "成功" : "失败"));
        System.out.println("使用方法: " + result.getMethod().getDescription());

        if (result.isSuccess()) {
            System.out.println("图片 URL: " + result.getUrl());
            assertEquals(ImageMethodEnum.EMOJI_PACK, result.getMethod());
            assertFalse(result.getUrl().contains("?"), "URL 不应该包含参数");
        }
    }
}

