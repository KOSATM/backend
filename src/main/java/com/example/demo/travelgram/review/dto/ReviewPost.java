package com.example.demo.travelgram.review.dto;

import java.time.OffsetDateTime;
import lombok.Data;

@Data
public class ReviewPost {
    private Long id;
    private String content;
    private Boolean isPosted;
    private String reviewPostUrl;
    private OffsetDateTime createdAt;
    private Long travelPlanId;
    private Long reviewStyleId;
}
