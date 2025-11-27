package com.example.demo.common.global;

import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.example.demo.common.global.exception.BusinessException;
import com.example.demo.common.global.exception.errorcode.BaseErrorCode;
import com.example.demo.common.global.response.ErrorResponse;

import lombok.extern.slf4j.Slf4j;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /* 비즈니스 예외 처리 */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ResponseWrapper<?>> handleBusinessException(BusinessException e) {
        BaseErrorCode baseErrorCode = e.getBaseErrorCode();

        return ResponseEntity
                .status(baseErrorCode.getStatus())
                .body(ResponseWrapper.error(
                        baseErrorCode.getStatus(),
                        ErrorResponse.from(baseErrorCode)));
    }

    /* 유효성 검사 실패 (DTO @Valid) */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ResponseWrapper<?>> handleValidationException(MethodArgumentNotValidException e) {

        String message = e.getBindingResult()
                .getAllErrors()
                .stream()
                .findFirst()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .orElse("유효성 검사에 실패했습니다.");

        e.printStackTrace();

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ResponseWrapper.error(
                        HttpStatus.BAD_REQUEST,
                        ErrorResponse.of("VALIDATION_ERROR", message)));
    }

    /* @RequestParam, @PathVariable 등 타입 불일치 */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ResponseWrapper<?>> handleTypeMismatch(MethodArgumentTypeMismatchException e) {

        e.printStackTrace();

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ResponseWrapper.error(
                        HttpStatus.BAD_REQUEST,
                        ErrorResponse.of("TYPE_MISMATCH", "요청 파라미터 타입이 올바르지 않습니다.")));
    }

    /* 지원하지 않는 HTTP Method 요청 */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ResponseWrapper<?>> handleMethodNotAllowed(HttpRequestMethodNotSupportedException e) {

        e.printStackTrace();

        return ResponseEntity
                .status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ResponseWrapper.error(
                        HttpStatus.METHOD_NOT_ALLOWED,
                        ErrorResponse.of("METHOD_NOT_ALLOWED", "허용되지 않은 HTTP 메서드입니다.")));
    }

    /* 예상하지 못한 모든 예외 처리 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ResponseWrapper<?>> handleException(Exception e) {

        e.printStackTrace();

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ResponseWrapper.error(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        ErrorResponse.of("INTERNAL_ERROR", "서버 내부 오류가 발생했습니다.")));
    }
}
