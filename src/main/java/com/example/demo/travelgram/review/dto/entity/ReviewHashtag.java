package com.example.demo.travelgram.review.dto.entity;

import java.time.OffsetDateTime;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class ReviewHashtag {
    private Long id;
    private String name;
    private OffsetDateTime createdAt;
    private Long hashtagGroupId;
}
