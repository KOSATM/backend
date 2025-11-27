package com.example.demo.common.global.exception.errorcode;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor

public enum SampleErrorCode implements BaseErrorCode {
    /*
        1. 해당 enum 복사해서 도메인에 맞는 이름으로 수정
        2. 도메인에 맞는 에러코드 선언
        3. throw new BusinessException(enum이름.해당예외코드);
    */
    
    // 400 Bad Request
    ERROR_NAME(HttpStatus.BAD_REQUEST, "여기다 예외 메시지 입력하세요");

    private final HttpStatus status;
    private final String message;
}