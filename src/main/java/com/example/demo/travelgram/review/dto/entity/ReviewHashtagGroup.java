package com.example.demo.travelgram.review.dto.entity;

import java.time.OffsetDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor  // MyBatis가 기본 생성자를 사용해 객체를 생성하고 setter/field 주입을 하게 함
@AllArgsConstructor // Builder 패턴을 위해 필요
@Builder
@Getter
public class ReviewHashtagGroup {
    private Long id;
    private OffsetDateTime createdAt;
    private Long reviewPostId;
}
