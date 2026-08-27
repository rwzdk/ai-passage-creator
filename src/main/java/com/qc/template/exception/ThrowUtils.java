package com.qc.template.exception;

/**
 * 寮傚父鎶涘嚭宸ュ叿绫?
 *
 */
public class ThrowUtils {

    /**
     * 鏉′欢鎴愮珛鍒欐姏鍑哄紓甯?
     *
     * @param condition
     * @param runtimeException
     */
    public static void throwIf(boolean condition, RuntimeException runtimeException) {
        if (condition) {
            throw runtimeException;
        }
    }

    /**
     * 鏉′欢鎴愮珛鍒欐姏寮傚父
     *
     * @param condition 鏉′欢
     * @param errorCode 閿欒鐮?
     */
    public static void throwIf(boolean condition, ErrorCode errorCode) {
        throwIf(condition, new BusinessException(errorCode));
    }

    /**
     * 鏉′欢鎴愮珛鍒欐姏寮傚父
     *
     * @param condition 鏉′欢
     * @param errorCode 閿欒鐮?
     * @param message   閿欒淇℃伅
     */
    public static void throwIf(boolean condition, ErrorCode errorCode, String message) {
        throwIf(condition, new BusinessException(errorCode, message));
    }
}
