package com.qc.template.manager;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.qc.template.constant.ArticleConstant.SSE_RECONNECT_TIME_MS;
import static com.qc.template.constant.ArticleConstant.SSE_TIMEOUT_MS;

/**
 * SSE Emitter 绠＄悊鍣?
 *
 */
@Component
@Slf4j
public class SseEmitterManager {

    /**
     * 瀛樺偍鎵€鏈夌殑 SseEmitter
     */
    private final Map<String, SseEmitter> emitterMap = new ConcurrentHashMap<>();

    /**
     * 鍒涘缓 SseEmitter
     *
     * @param taskId 浠诲姟ID
     * @return SseEmitter
     */
    public SseEmitter createEmitter(String taskId) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        
        // 璁剧疆瓒呮椂鍥炶皟
        emitter.onTimeout(() -> {
            log.warn("SSE 杩炴帴瓒呮椂, taskId={}", taskId);
            emitterMap.remove(taskId);
        });
        
        // 璁剧疆瀹屾垚鍥炶皟
        emitter.onCompletion(() -> {
            log.info("SSE 杩炴帴瀹屾垚, taskId={}", taskId);
            emitterMap.remove(taskId);
        });
        
        // 璁剧疆閿欒鍥炶皟
        emitter.onError((e) -> {
            log.error("SSE 杩炴帴閿欒, taskId={}", taskId, e);
            emitterMap.remove(taskId);
        });
        
        emitterMap.put(taskId, emitter);
        log.info("SSE 杩炴帴宸插垱寤? taskId={}", taskId);
        
        return emitter;
    }

    /**
     * 鍙戦€佹秷鎭?
     *
     * @param taskId  浠诲姟ID
     * @param message 娑堟伅鍐呭
     */
    public void send(String taskId, String message) {
        SseEmitter emitter = emitterMap.get(taskId);
        if (emitter == null) {
            log.warn("SSE Emitter 涓嶅瓨鍦? taskId={}", taskId);
            return;
        }
        
        try {
            emitter.send(SseEmitter.event()
                    .data(message)
                    .reconnectTime(SSE_RECONNECT_TIME_MS));
            log.debug("SSE 娑堟伅鍙戦€佹垚鍔? taskId={}, message={}", taskId, message);
        } catch (IOException e) {
            log.error("SSE 娑堟伅鍙戦€佸け璐? taskId={}", taskId, e);
            emitterMap.remove(taskId);
        }
    }

    /**
     * 瀹屾垚杩炴帴
     *
     * @param taskId 浠诲姟ID
     */
    public void complete(String taskId) {
        SseEmitter emitter = emitterMap.get(taskId);
        if (emitter == null) {
            log.warn("SSE Emitter 涓嶅瓨鍦? taskId={}", taskId);
            return;
        }
        
        try {
            emitter.complete();
            log.info("SSE 杩炴帴宸插畬鎴? taskId={}", taskId);
        } catch (Exception e) {
            log.error("SSE 杩炴帴瀹屾垚澶辫触, taskId={}", taskId, e);
        } finally {
            emitterMap.remove(taskId);
        }
    }

    /**
     * 妫€鏌?Emitter 鏄惁瀛樺湪
     *
     * @param taskId 浠诲姟ID
     * @return 鏄惁瀛樺湪
     */
    public boolean exists(String taskId) {
        return emitterMap.containsKey(taskId);
    }
}
