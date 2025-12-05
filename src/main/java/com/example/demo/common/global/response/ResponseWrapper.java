package com.example.demo.common.global.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Global Response Wrapper
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResponseWrapper<T> {
    private int code;
    private String message;
    private T data;
    private boolean success;

    public static <T> ResponseWrapper<T> success(String message, T data) {
        return ResponseWrapper.<T>builder()
                .code(200)
                .message(message)
                .data(data)
                .success(true)
                .build();
    }

    public static <T> ResponseWrapper<T> error(String message) {
        return ResponseWrapper.<T>builder()
                .code(400)
                .message(message)
                .success(false)
                .build();
    }

    public static <T> ResponseWrapper<T> error(int code, String message) {
        return ResponseWrapper.<T>builder()
                .code(code)
                .message(message)
                .success(false)
                .build();
    }
}
