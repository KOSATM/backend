package com.example.demo.planner.plan.dto.request;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 새로운 장소를 추가할 때 LLM이 제공하는 장소 정보.
 * 
 * <p>ADD_PLACE 액션의 payload로 사용된다.
 * 
 * <p><strong>Level 1 필수 필드:</strong>
 * <ul>
 *   <li>placeName (필수) - 장소 식별</li>
 *   <li>startTime, endTime (필수) - 시간 충돌 판단</li>
 * </ul>
 * 
 * <p><strong>Level 2 선택 필드:</strong>
 * <ul>
 *   <li>address, lat, lng - 위치 기반 추천</li>
 *   <li>expectedCost - 예산 판단</li>
 *   <li>normalizedCategory - 카테고리 필터링</li>
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlacePayload {
    // === Level 1: 결정 필수 ===
    private String placeName;
    private String startTime;  // HH:mm 형식
    private String endTime;    // HH:mm 형식
    
    // === Level 2: 판단 보조 ===
    private String address;
    private Double lat;
    private Double lng;
    private BigDecimal expectedCost;
    private String normalizedCategory;
    
    // === 부가 정보 ===
    private String firstImage;
    private String firstImage2;
    private String memo;
}
