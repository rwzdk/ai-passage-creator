package com.qc.template.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Type;
import java.time.LocalDateTime;

/**
 * Gson 宸ュ叿绫?
 * 鎻愪緵缁熶竴鐨?Gson 瀹炰緥锛岄伩鍏嶉噸澶嶅垱寤?
 *
 */
@Slf4j
public class GsonUtils {

    /**
     * 鍗曚緥 Gson 瀹炰緥
     */
    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class,
                    (JsonSerializer<LocalDateTime>) (value, type, context) ->
                            new JsonPrimitive(value.toString()))
            .registerTypeAdapter(LocalDateTime.class,
                    (JsonDeserializer<LocalDateTime>) (json, type, context) ->
                            LocalDateTime.parse(json.getAsString()))
            .create();

    private GsonUtils() {
        // 绉佹湁鏋勯€犲嚱鏁帮紝闃叉瀹炰緥鍖?
    }

    /**
     * 鑾峰彇 Gson 瀹炰緥
     *
     * @return Gson 瀹炰緥
     */
    public static Gson getInstance() {
        return GSON;
    }

    /**
     * 瀵硅薄杞?JSON 瀛楃涓?
     *
     * @param obj 对象
     * @return JSON 瀛楃涓?
     */
    public static String toJson(Object obj) {
        if (obj == null) {
            return null;
        }
        return GSON.toJson(obj);
    }

    /**
     * JSON 字符串转对象
     *
     * @param json  JSON 瀛楃涓?
     * @param clazz 鐩爣绫诲瀷
     * @param <T>   娉涘瀷绫诲瀷
     * @return 对象实例
     */
    public static <T> T fromJson(String json, Class<T> clazz) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        return GSON.fromJson(json, clazz);
    }

    /**
     * JSON 字符串转对象（支持泛型）
     *
     * @param json      JSON 瀛楃涓?
     * @param typeToken TypeToken 绫诲瀷寮曠敤
     * @param <T>       娉涘瀷绫诲瀷
     * @return 对象实例
     */
    public static <T> T fromJson(String json, TypeToken<T> typeToken) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        return GSON.fromJson(json, typeToken.getType());
    }

    /**
     * JSON 瀛楃涓茶浆瀵硅薄锛堟敮鎸?Type锛?
     *
     * @param json JSON 瀛楃涓?
     * @param type Type 绫诲瀷
     * @param <T>  娉涘瀷绫诲瀷
     * @return 对象实例
     */
    public static <T> T fromJson(String json, Type type) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        return GSON.fromJson(json, type);
    }

    /**
     * 瀹夊叏鍦板皢 JSON 瀛楃涓茶浆涓哄璞★紝瑙ｆ瀽澶辫触鏃惰繑鍥?null
     *
     * @param json  JSON 瀛楃涓?
     * @param clazz 鐩爣绫诲瀷
     * @param <T>   娉涘瀷绫诲瀷
     * @return 瀵硅薄瀹炰緥锛岃В鏋愬け璐ヨ繑鍥?null
     */
    public static <T> T fromJsonSafe(String json, Class<T> clazz) {
        try {
            return fromJson(json, clazz);
        } catch (JsonSyntaxException e) {
            log.error("JSON 瑙ｆ瀽澶辫触, json={}", json, e);
            return null;
        }
    }

    /**
     * 瀹夊叏鍦板皢 JSON 瀛楃涓茶浆涓哄璞★紙鏀寔娉涘瀷锛夛紝瑙ｆ瀽澶辫触鏃惰繑鍥?null
     *
     * @param json      JSON 瀛楃涓?
     * @param typeToken TypeToken 绫诲瀷寮曠敤
     * @param <T>       娉涘瀷绫诲瀷
     * @return 瀵硅薄瀹炰緥锛岃В鏋愬け璐ヨ繑鍥?null
     */
    public static <T> T fromJsonSafe(String json, TypeToken<T> typeToken) {
        try {
            return fromJson(json, typeToken);
        } catch (JsonSyntaxException e) {
            log.error("JSON 瑙ｆ瀽澶辫触, json={}", json, e);
            return null;
        }
    }
}
