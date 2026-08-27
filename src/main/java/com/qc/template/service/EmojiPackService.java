package com.qc.template.service;

import cn.hutool.core.util.StrUtil;
import com.qc.template.config.EmojiPackConfig;
import com.qc.template.model.enums.ImageMethodEnum;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import static com.qc.template.constant.ArticleConstant.PICSUM_URL_TEMPLATE;

/**
 * 琛ㄦ儏鍖呮绱㈡湇鍔★紙鍩轰簬 Bing 鍥剧墖鎼滅储锛?
 * 绋嬪簭鑷姩鍦ㄥ叧閿瘝鍚庢嫾鎺?琛ㄦ儏鍖?杩涜鎼滅储
 *
 */
@Service
@Slf4j
public class EmojiPackService implements ImageSearchService {

    @Resource
    private EmojiPackConfig emojiPackConfig;

    @Override
    public String searchImage(String keywords) {
        if (StrUtil.isBlank(keywords)) {
            log.warn("琛ㄦ儏鍖呮悳绱㈠叧閿瘝涓虹┖");
            return null;
        }

        try {
            // 1. 鏋勫缓鎼滅储璇嶏紙绋嬪簭鍥哄畾鎷兼帴"琛ㄦ儏鍖?锛?
            String searchText = keywords + emojiPackConfig.getSuffix();
            log.info("琛ㄦ儏鍖呮悳绱? {} -> {}", keywords, searchText);

            // 2. 鏋勫缓鎼滅储 URL
            String fetchUrl = buildSearchUrl(searchText);

            // 3. 浣跨敤 Jsoup 鑾峰彇椤甸潰
            Document document = Jsoup.connect(fetchUrl)
                    .timeout(emojiPackConfig.getTimeout())
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .get();

            // 4. 瀹氫綅鍥剧墖瀹瑰櫒
            Element div = document.getElementsByClass("dgControl").first();
            if (div == null) {
                log.warn("Bing 鏈壘鍒板浘鐗囧鍣? keywords={}", keywords);
                return null;
            }

            // 5. 浣跨敤 CSS 閫夋嫨鍣ㄦ彁鍙栧浘鐗?
            Elements imgElements = div.select("img.mimg");
            if (imgElements.isEmpty()) {
                log.warn("Bing 鏈绱㈠埌琛ㄦ儏鍖? keywords={}, searchText={}", keywords, searchText);
                return null;
            }

            // 6. 鑾峰彇绗竴寮犲浘鐗?URL
            String imageUrl = imgElements.get(0).attr("src");
            if (StrUtil.isBlank(imageUrl)) {
                log.warn("鍥剧墖 URL 涓虹┖, keywords={}", keywords);
                return null;
            }

            // 7. 娓呯悊 URL 鍙傛暟锛堢Щ闄??w=xxx&h=xxx锛?
            imageUrl = cleanImageUrl(imageUrl);

            log.info("琛ㄦ儏鍖呮绱㈡垚鍔? {} -> {}", keywords, imageUrl);
            return imageUrl;

        } catch (Exception e) {
            log.error("琛ㄦ儏鍖呮绱㈠紓甯? keywords={}", keywords, e);
            return null;
        }
    }

    @Override
    public ImageMethodEnum getMethod() {
        return ImageMethodEnum.EMOJI_PACK;
    }

    @Override
    public String getFallbackImage(int position) {
        return String.format(PICSUM_URL_TEMPLATE, position);
    }

    /**
     * 鏋勫缓 Bing 鍥剧墖鎼滅储 URL
     *
     * @param searchText 鎼滅储鏂囨湰
     * @return 瀹屾暣鐨勬悳绱?URL
     */
    private String buildSearchUrl(String searchText) {
        String encodedText = URLEncoder.encode(searchText, StandardCharsets.UTF_8);
        // 蹇呴』娣诲姞 mmasync=1 鍙傛暟
        return String.format("%s?q=%s&mmasync=1", 
                emojiPackConfig.getSearchUrl(), 
                encodedText);
    }

    /**
     * 娓呯悊鍥剧墖 URL 鍙傛暟
     * 绉婚櫎 ?w=xxx&h=xxx 绛夊弬鏁帮紝閬垮厤鍥剧墖璐ㄩ噺涓嬮檷鍜岃闂棶棰?
     *
     * @param url 鍘熷鍥剧墖 URL
     * @return 娓呯悊鍚庣殑 URL
     */
    private String cleanImageUrl(String url) {
        if (StrUtil.isBlank(url)) {
            return url;
        }
        
        int questionMarkIndex = url.indexOf("?");
        if (questionMarkIndex > 0) {
            return url.substring(0, questionMarkIndex);
        }
        
        return url;
    }
}
