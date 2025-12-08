package com.example.demo.common.chat.intent.dto.request;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * 입력 필터링 에이전트가 생성하는 정규화된 요청 객체
 * IntentAnalysisAgent가 더 정확한 의도 분석을 할 수 있도록 전처리된 데이터 제공
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class FilteredRequest {

    /**
     * 필터링 통과 여부
     * true: 욕설/불법/위험 콘텐츠 감지 (처리 중단)
     * false: 정상 처리 가능
     */
    private boolean isBlocked;

    /**
     * 블로킹된 경우 사유 목록
     */
    private java.util.List<String> blockReasons;

    /**
     * 원본 사용자 메시지
     */
    private String rawText;

    /**
     * 정규화된 텍스트 (IntentAnalysisAgent가 사용)
     * - 날짜/시간 표현 정규화
     * - 장소 오타 보정
     * - 숫자/단위 표준화
     */
    private String normalizedText;

    /**
     * 추출된 구조화 엔티티
     * - date: ISO8601 날짜 (YYYY-MM-DD)
     * - time: HH:mm 형식 시간
     * - places: 장소명 배열
     * - numbers: 추출된 숫자값
     * - duration: 기간/시간 (분 단위)
     */
    private Map<String, Object> entities;

    /**
     * 현재 URL (원본 유지)
     */
    private String currentUrl;

    /**
     * 블로킹되지 않은 정상 요청인지 확인
     */
    public boolean isValid() {
        return !isBlocked;
    }

    /**
     * IntentRequest로 변환 (IntentAnalysisAgent용)
     * normalizedText를 userMessage로 사용
     */
    public IntentRequest toIntentRequest() {
        return IntentRequest.builder()
                .userMessage(normalizedText != null ? normalizedText : rawText)
                .currentUrl(currentUrl)
                .build();
    }
}
