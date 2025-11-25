package com.example.demo.travelgram.review.dto.entity;

import java.time.OffsetDateTime;

import lombok.Getter;

@Getter
public class ReviewPhotoGroup {
    private Long id;
    private OffsetDateTime createdAt;
    private Long reviewPostId;
}
