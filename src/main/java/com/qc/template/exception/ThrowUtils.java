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
     * 条件成立则抛异常
     *
     * @param condition 条件
     * @param errorCode 閿欒鐮?
     */
    public static void throwIf(boolean condition, ErrorCode errorCode) {
        throwIf(condition, new BusinessException(errorCode));
    }

    /**
     * 条件成立则抛异常
     *
     * @param condition 条件
     * @param errorCode 閿欒鐮?
     * @param message   閿欒淇℃伅
     */
    public static void throwIf(boolean condition, ErrorCode errorCode, String message) {
        throwIf(condition, new BusinessException(errorCode, message));
    }
}
