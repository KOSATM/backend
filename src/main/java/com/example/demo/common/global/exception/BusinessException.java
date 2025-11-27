package com.example.demo.common.global.exception;

import com.example.demo.common.global.exception.errorcode.BaseErrorCode;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

    private final BaseErrorCode baseErrorCode;

    public BusinessException(BaseErrorCode baseErrorCode) {
        super(baseErrorCode.getMessage());
        this.baseErrorCode = baseErrorCode;
    }
}
