package com.qc.template.exception;

import lombok.Getter;

/**
 * 鑷畾涔変笟鍔″紓甯?
 *
 */
@Getter
public class BusinessException extends RuntimeException {

    /**
     * 閿欒鐮?
     */
    private final int code;

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
    }
}
