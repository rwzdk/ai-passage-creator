package com.qc.template.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.qc.template.config.IconifyConfig;
import com.qc.template.model.enums.ImageMethodEnum;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import static com.qc.template.constant.ArticleConstant.PICSUM_URL_TEMPLATE;

/**
 * Iconify 鍥炬爣搴撴绱㈡湇鍔?
 * 鎻愪緵 275k+ 寮€婧愬浘鏍囨绱㈠拰 SVG 鐢熸垚
 *
 */
@Service
@Slf4j
public class IconifyService implements ImageSearchService {

    @Resource
    private IconifyConfig iconifyConfig;

    private final OkHttpClient httpClient = new OkHttpClient();

    @Override
    public String searchImage(String keywords) {
        if (keywords == null || keywords.trim().isEmpty()) {
            log.warn("Iconify 鎼滅储鍏抽敭璇嶄负绌?);
            return null;
        }

        try {
            // 1. 鎼滅储鍥炬爣
            String searchUrl = buildSearchUrl(keywords);
            String searchResult = callApi(searchUrl);

            if (searchResult == null) {
                return null;
            }

            // 2. 瑙ｆ瀽缁撴灉锛岃幏鍙栫涓€涓浘鏍?
            String iconName = extractFirstIcon(searchResult);
            if (iconName == null) {
                log.warn("Iconify 鏈绱㈠埌鍥炬爣: {}", keywords);
                return null;
            }

            // 3. 鏋勫缓 SVG URL
            String svgUrl = buildSvgUrl(iconName);
            log.info("Iconify 鍥炬爣妫€绱㈡垚鍔? {} -> {}", keywords, iconName);
            
            return svgUrl;

        } catch (Exception e) {
            log.error("Iconify 鍥炬爣妫€绱㈠紓甯? keywords={}", keywords, e);
            return null;
        }
    }

    @Override
    public ImageMethodEnum getMethod() {
        return ImageMethodEnum.ICONIFY;
    }

    @Override
    public String getFallbackImage(int position) {
        return String.format(PICSUM_URL_TEMPLATE, position);
    }

    /**
     * 鏋勫缓鎼滅储 URL
     */
    private String buildSearchUrl(String keywords) {
        String encodedKeywords = URLEncoder.encode(keywords, StandardCharsets.UTF_8);
        return String.format("%s/search?query=%s&limit=%d",
                iconifyConfig.getApiUrl(),
                encodedKeywords,
                iconifyConfig.getSearchLimit());
    }

    /**
     * 璋冪敤 Iconify API
     */
    private String callApi(String url) {
        try {
            Request request = new Request.Builder()
                    .url(url)
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    log.error("Iconify API 璋冪敤澶辫触: {}", response.code());
                    return null;
                }

                return response.body().string();
            }
        } catch (IOException e) {
            log.error("Iconify API 璋冪敤寮傚父", e);
            return null;
        }
    }

    /**
     * 浠庢悳绱㈢粨鏋滀腑鎻愬彇绗竴涓浘鏍囧悕绉?
     */
    private String extractFirstIcon(String jsonResponse) {
        try {
            JsonObject json = JsonParser.parseString(jsonResponse).getAsJsonObject();
            JsonArray icons = json.getAsJsonArray("icons");

            if (icons == null || icons.isEmpty()) {
                return null;
            }

            return icons.get(0).getAsString();
        } catch (Exception e) {
            log.error("瑙ｆ瀽 Iconify 鎼滅储缁撴灉澶辫触", e);
            return null;
        }
    }

    /**
     * 鏋勫缓 SVG URL
     *
     * @param iconName 鍥炬爣鍚嶇О锛堟牸寮忥細prefix:name锛屽 mdi:home锛?
     * @return SVG URL
     */
    private String buildSvgUrl(String iconName) {
        // 灏?"mdi:home" 杞崲涓?"mdi/home"
        String path = iconName.replace(":", "/");

        StringBuilder url = new StringBuilder(iconifyConfig.getApiUrl())
                .append("/")
                .append(path)
                .append(".svg");

        // 娣诲姞楂樺害鍙傛暟
        boolean hasParams = false;
        if (iconifyConfig.getDefaultHeight() != null && iconifyConfig.getDefaultHeight() > 0) {
            url.append("?height=").append(iconifyConfig.getDefaultHeight());
            hasParams = true;
        }

        // 娣诲姞棰滆壊鍙傛暟锛堝鏋滈厤缃簡锛?
        if (iconifyConfig.getDefaultColor() != null && !iconifyConfig.getDefaultColor().isEmpty()) {
            url.append(hasParams ? "&" : "?");
            
            // 澶勭悊棰滆壊鏍煎紡锛堝 #000000 闇€瑕佽浆涓?%23000000锛?
            String color = iconifyConfig.getDefaultColor();
            if (color.startsWith("#")) {
                color = "%23" + color.substring(1);
            }
            url.append("color=").append(color);
        }

        return url.toString();
    }
}
