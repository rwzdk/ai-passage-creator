package com.yupi.template.service.impl;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.yupi.template.config.GNewsConfig;
import com.yupi.template.model.vo.HotTopicItemVO;
import com.yupi.template.model.vo.HotTopicsVO;
import com.yupi.template.service.HotTopicService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class HotTopicServiceImpl implements HotTopicService {

    private static final List<String> FALLBACK_TOPICS = List.of(
            "AI 如何改变普通人的工作方式",
            "新能源汽车进入智能化下半场",
            "年轻人如何建立长期主义的学习系统",
            "远程办公之后，团队协作发生了什么变化",
            "健康生活方式正在经历哪些新变化",
            "普通人如何提升职场竞争力",
            "生成式 AI 如何影响内容创作",
            "如何把复杂问题讲清楚"
    );

    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(Duration.ofSeconds(5))
            .readTimeout(Duration.ofSeconds(8))
            .callTimeout(Duration.ofSeconds(10))
            .build();

    @Resource
    private GNewsConfig gNewsConfig;

    private volatile HotTopicsVO cachedTopics;
    private volatile long cachedAt;

    @Override
    public HotTopicsVO getHotTopics() {
        return getHotTopics(false);
    }

    @Override
    public HotTopicsVO getHotTopics(boolean refresh) {
        if (!refresh && isCacheValid()) {
            return cachedTopics;
        }
        if (gNewsConfig.getApiKey() == null || gNewsConfig.getApiKey().isBlank()) {
            return fallbackTopics();
        }
        try {
            HotTopicsVO result = requestTopics();
            if (result.getItems() != null && !result.getItems().isEmpty()) {
                cachedTopics = result;
                cachedAt = System.currentTimeMillis();
                return result;
            }
        } catch (Exception e) {
            log.warn("获取实时热门选题失败，将使用本地备用选题: {}", e.getMessage());
        }
        return fallbackTopics();
    }

    private boolean isCacheValid() {
        return cachedTopics != null
                && System.currentTimeMillis() - cachedAt < Duration.ofMinutes(Math.max(1, gNewsConfig.getCacheMinutes())).toMillis();
    }

    HotTopicsVO requestTopics() throws IOException {
        String baseUrl = gNewsConfig.getBaseUrl();
        HttpUrl base = HttpUrl.parse(baseUrl.endsWith("/") ? baseUrl : baseUrl + "/");
        if (base == null) {
            throw new IOException("GNews base URL 无效");
        }
        HttpUrl url = base.newBuilder()
                .addPathSegment("top-headlines")
                .addQueryParameter("category", "general")
                .addQueryParameter("lang", "zh")
                .addQueryParameter("country", "cn")
                .addQueryParameter("max", String.valueOf(Math.min(10, Math.max(1, gNewsConfig.getMaxItems()))))
                .addQueryParameter("apikey", gNewsConfig.getApiKey())
                .build();
        Request request = new Request.Builder().url(url).get().build();
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                throw new IOException("GNews HTTP " + response.code());
            }
            return parseResponse(response.body().string());
        }
    }

    public HotTopicsVO parseResponse(String body) {
        JsonObject root = JsonParser.parseString(body).getAsJsonObject();
        JsonArray articles = root.getAsJsonArray("articles");
        List<HotTopicItemVO> items = new ArrayList<>();
        if (articles != null) {
            for (JsonElement element : articles) {
                JsonObject article = element.getAsJsonObject();
                String title = getString(article, "title");
                if (title == null || title.isBlank()) continue;
                JsonObject sourceObject = article.has("source") && article.get("source").isJsonObject()
                        ? article.getAsJsonObject("source") : null;
                String source = sourceObject == null ? "实时热点" : getString(sourceObject, "name");
                items.add(HotTopicItemVO.builder()
                        .title(title.trim())
                        .source(source == null || source.isBlank() ? "实时热点" : source)
                        .publishedAt(parseDate(getString(article, "publishedAt")))
                        .url(getString(article, "url"))
                        .build());
            }
        }
        return HotTopicsVO.builder().source("gnews").updatedAt(LocalDateTime.now()).items(items).build();
    }

    private String getString(JsonObject object, String key) {
        return object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsString() : null;
    }

    private LocalDateTime parseDate(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return OffsetDateTime.parse(value).toLocalDateTime();
        } catch (DateTimeParseException ignored) {
            try { return LocalDateTime.parse(value); } catch (DateTimeParseException ignoredAgain) { return null; }
        }
    }

    private HotTopicsVO fallbackTopics() {
        List<HotTopicItemVO> items = FALLBACK_TOPICS.stream()
                .map(title -> HotTopicItemVO.builder().title(title).source("推荐选题").build())
                .toList();
        return HotTopicsVO.builder().source("fallback").updatedAt(LocalDateTime.now()).items(items).build();
    }
}
