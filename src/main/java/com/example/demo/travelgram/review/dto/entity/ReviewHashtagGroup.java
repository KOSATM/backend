package com.example.demo.travelgram.review.dto.entity;

import java.time.OffsetDateTime;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class ReviewHashtagGroup {
    private Long id;
    private OffsetDateTime createdAt;
    private Long reviewPostId;
}
