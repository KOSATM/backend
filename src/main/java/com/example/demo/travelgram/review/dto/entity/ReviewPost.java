package com.example.demo.travelgram.review.dto.entity;

import java.time.OffsetDateTime;
import lombok.Getter;

@Getter
public class ReviewPost {
    private Long id;
    private String content;
    private Boolean isPosted;
    private String reviewPostUrl;
    private OffsetDateTime createdAt;
    private Long travelPlanId;
    private Long reviewStyleId;
}
