package com.example.demo.travelgram.review.dto.entity;

import java.time.OffsetDateTime;

import lombok.Builder;
import lombok.Getter;

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
