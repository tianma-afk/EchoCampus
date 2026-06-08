package com.echocampus.shared.exception;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class BusinessException extends RuntimeException {
    private final String code;
    private final ErrorCode errorCode;

    //错误码
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
        this.errorCode = errorCode;
    }

    //错误详情
    public BusinessException(ErrorCode errorCode, String detail) {
        super(errorCode.getMessage() + ": " + detail);
        this.code = errorCode.getCode();
        this.errorCode = errorCode;
    }

    //错误原因
    public BusinessException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.code = errorCode.getCode();
        this.errorCode = errorCode;
    }
}
