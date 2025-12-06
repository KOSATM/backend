package com.example.demo.travelgram.review.dto.entity;

import java.time.OffsetDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor  // ✅ MyBatis가 "이름 기반"으로 매핑할 수 있게 빈 생성자 허용
@AllArgsConstructor // ✅ @Builder는 전체 생성자가 필요하므로 세트로 추가
@Builder
@Getter
public class ReviewPhoto {
    private Long id;
    private Long photoGroupId;
    private String fileUrl;
    private Integer orderIndex;
    private String summary;
    private OffsetDateTime createdAt;
}
