package com.example.demo.common.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;


import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class JsonParser {
    private final ObjectMapper objectMapper;
    private final Validator validator;

    // json -> list<dto>로 변환해주는 메서드
    public <T> List<T> parseJsonToList(String jsonResponse, TypeReference<List<T>> typeReference) {

        // 빈 값 처리
        if (jsonResponse == null || jsonResponse.trim().isEmpty() || jsonResponse.trim().equals("[]")) {
            log.warn("빈 JSON 응답 받음");
            return Collections.emptyList();
        }

        String cleanedJson = jsonResponse.trim();
        if (cleanedJson.startsWith("```json")) {
            cleanedJson = cleanedJson.substring(7); // "```json" 제거
        }
        if (cleanedJson.startsWith("```")) {
            cleanedJson = cleanedJson.substring(3); // "```" 제거
        }
        if (cleanedJson.endsWith("```")) {
            cleanedJson = cleanedJson.substring(0, cleanedJson.length() - 3);
        }
        cleanedJson = cleanedJson.trim(); // 앞뒤 공백 최종 제거

        if (cleanedJson.isEmpty() || cleanedJson.equals("[]")) {
            return Collections.emptyList();
        }

        // 정제된 JSON 문자열을 사용
        try {
            JsonNode rootNode = objectMapper.readTree(cleanedJson);

            // 배열인지 확인
            if (!rootNode.isArray()) {
                log.warn("예상된 JSON 배열 형식이 아닙니다: {}", cleanedJson);
                return Collections.emptyList();
            }

            // 1) JSON 전체를 List<T>로 변환
            List<T> list = objectMapper.convertValue(rootNode, typeReference);

            // 2) bean validation
            List<T> validList = new ArrayList<>();

            for (T item : list) {
                Set<ConstraintViolation<T>> violations = validator.validate(item);

                if (violations.isEmpty()) {
                    validList.add(item);
                } else {
                    log.warn("DTO 유효성 검증 실패. 객체: {}, 위반: {}", item, violations);
                }
            }
            return validList;
        } catch (Exception e) {
            log.error("JSON 파싱 실패: {}", jsonResponse, e);
            return Collections.emptyList();
        }
    }
}
