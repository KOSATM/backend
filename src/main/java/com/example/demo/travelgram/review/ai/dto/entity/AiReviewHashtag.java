package com.example.demo.travelgram.review.ai.dto.entity;

import java.time.OffsetDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
public class AiReviewHashtag {
    private Long id;
    private String name;
    private OffsetDateTime createdAt;
    private Long reviewStyleId;
}
