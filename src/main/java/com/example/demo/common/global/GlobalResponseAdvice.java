package com.example.demo.common.global;

import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import com.example.demo.common.global.annotation.NoWrap;

@RestControllerAdvice
public class GlobalResponseAdvice implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {

        // @NoWrap 적용 시 래핑 제외
        if (returnType.hasMethodAnnotation(NoWrap.class) ||
                returnType.getContainingClass().isAnnotationPresent(NoWrap.class)) {
            return false;
        }

        return true;
    }

    @Override
    public Object beforeBodyWrite(
            Object body,
            MethodParameter returnType,
            MediaType contentType,
            Class<? extends HttpMessageConverter<?>> converterType,
            ServerHttpRequest request,
            ServerHttpResponse response) {

        // 예외 상황은 Handler가 처리 → Advice가 관여하면 안 됨
        if (body instanceof ResponseWrapper) {
            return body;
        }

        // 2) String 응답은 절대 래핑 금지!!
        if (body instanceof String ||
                converterType == StringHttpMessageConverter.class) {
            return body;
        }

        // HTTP 상태 읽기
        int status = ((ServletServerHttpResponse) response).getServletResponse().getStatus();

        // 204 NO_CONTENT 는 body 없어야 함
        if (status == HttpStatus.NO_CONTENT.value()) {
            return null;
        }

        // body가 null이면 감싸지 않음 (204 등)
        if (body == null)
            return null;

        // 정상 응답은 감싸기
        return ResponseWrapper.ok(HttpStatus.valueOf(status), body);
    }
}
