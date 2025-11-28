package com.example.demo.common.global.response;

import com.example.demo.common.global.exception.errorcode.BaseErrorCode;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ErrorResponse {
    private String code;
    private String message;

    // 일반적인 메시지가 필요할 때 사용
    public static ErrorResponse from(BaseErrorCode baseErrorCode) {
        return new ErrorResponse(baseErrorCode.name(), baseErrorCode.getMessage());
    }
    
    // Validation 등의 커스텀 메시지가 필요할 때 사용
    public static ErrorResponse of(String code, String message) {
        return new ErrorResponse(code, message);
    }
}
