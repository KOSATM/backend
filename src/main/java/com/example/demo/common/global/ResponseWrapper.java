package com.example.demo.common.global;

import org.springframework.http.HttpStatus;

import com.example.demo.common.global.response.ErrorResponse;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ResponseWrapper<T> {
    private boolean success;
    private int status;
    private T data;
    private ErrorResponse error;

    public static <T> ResponseWrapper<T> ok(HttpStatus status, T data) {
        return new ResponseWrapper<>(true, status.value(), data, null);
    }

    public static ResponseWrapper<?> error(HttpStatus status, ErrorResponse error) {
        return new ResponseWrapper<>(false, status.value(), null, error);
    }
}