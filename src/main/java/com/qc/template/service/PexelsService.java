package com.qc.template.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.qc.template.config.PexelsConfig;
import com.qc.template.model.enums.ImageMethodEnum;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.io.IOException;

import static com.qc.template.constant.ArticleConstant.*;

/**
 * Pexels 鍥剧墖妫€绱㈡湇鍔?
 *
 */
@Service
@Slf4j
public class PexelsService implements ImageSearchService {

    @Resource
    private PexelsConfig pexelsConfig;

    private final OkHttpClient httpClient = new OkHttpClient();

    @Override
    public String searchImage(String keywords) {
        try {
            String url = buildSearchUrl(keywords);
            
            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Authorization", pexelsConfig.getApiKey())
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    log.error("Pexels API 璋冪敤澶辫触: {}", response.code());
                    return null;
                }

                String responseBody = response.body().string();
                return extractImageUrl(responseBody, keywords);
            }
        } catch (IOException e) {
            log.error("Pexels API 璋冪敤寮傚父", e);
            return null;
        }
    }

    @Override
    public ImageMethodEnum getMethod() {
        return ImageMethodEnum.PEXELS;
    }

    @Override
    public String getFallbackImage(int position) {
        return String.format(PICSUM_URL_TEMPLATE, position);
    }

    /**
     * 鏋勫缓鎼滅储 URL
     *
     * @param keywords 鎼滅储鍏抽敭璇?
     * @return 瀹屾暣鐨勬悳绱?URL
     */
    private String buildSearchUrl(String keywords) {
        return String.format("%s?query=%s&per_page=%d&orientation=%s",
                PEXELS_API_URL,
                keywords,
                PEXELS_PER_PAGE,
                PEXELS_ORIENTATION_LANDSCAPE);
    }

    /**
     * 浠庡搷搴斾腑鎻愬彇鍥剧墖 URL
     *
     * @param responseBody 鍝嶅簲浣?
     * @param keywords     鎼滅储鍏抽敭璇嶏紙鐢ㄤ簬鏃ュ織锛?
     * @return 鍥剧墖 URL锛屾湭鎵惧埌杩斿洖 null
     */
    private String extractImageUrl(String responseBody, String keywords) {
        JsonObject jsonObject = JsonParser.parseString(responseBody).getAsJsonObject();
        JsonArray photos = jsonObject.getAsJsonArray("photos");
        
        if (photos.isEmpty()) {
            log.warn("Pexels 鏈绱㈠埌鍥剧墖: {}", keywords);
            return null;
        }

        JsonObject photo = photos.get(0).getAsJsonObject();
        JsonObject src = photo.getAsJsonObject("src");
        return src.get("large").getAsString();
    }
}
