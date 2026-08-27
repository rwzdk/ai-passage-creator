package com.qc.template.service;

import com.qc.template.config.GNewsConfig;
import com.qc.template.model.vo.HotTopicsVO;
import com.qc.template.service.impl.HotTopicServiceImpl;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class HotTopicServiceTest {

    @Test
    void parsesGNewsArticles() {
        HotTopicServiceImpl service = new HotTopicServiceImpl();
        HotTopicsVO result = service.parseResponse("""
                {"articles":[{"title":"AI 正在改变内容创作","publishedAt":"2026-08-07T10:00:00Z","url":"https://example.com/a","source":{"name":"Example News"}}]}
                """);

        assertEquals("gnews", result.getSource());
        assertEquals(1, result.getItems().size());
        assertEquals("AI 正在改变内容创作", result.getItems().get(0).getTitle());
        assertEquals("Example News", result.getItems().get(0).getSource());
    }

    @Test
    void usesFallbackWhenApiKeyIsMissing() throws Exception {
        HotTopicServiceImpl service = new HotTopicServiceImpl();
        GNewsConfig config = new GNewsConfig();
        setConfig(service, config);

        HotTopicsVO result = service.getHotTopics();

        assertEquals("fallback", result.getSource());
        assertFalse(result.getItems().isEmpty());
    }

    @Test
    void refreshReturnsAnotherBatchWhenSourceDoesNotChange() throws Exception {
        HotTopicServiceImpl service = new HotTopicServiceImpl();
        GNewsConfig config = new GNewsConfig();
        setConfig(service, config);

        HotTopicsVO first = service.getHotTopics();
        HotTopicsVO refreshed = service.getHotTopics(true);

        assertNotEquals(
                first.getItems().stream().map(item -> item.getTitle()).toList(),
                refreshed.getItems().stream().map(item -> item.getTitle()).toList()
        );
    }

    private void setConfig(HotTopicServiceImpl service, GNewsConfig config) throws Exception {
        Field field = HotTopicServiceImpl.class.getDeclaredField("gNewsConfig");
        field.setAccessible(true);
        field.set(service, config);
    }
}
