package com.example.demo.dto.reviewPost;

import java.time.OffsetDateTime;
import lombok.Data;

@Data
public class RReviewPost {
    private Long id;
    private String content;
    private Boolean isPosted;
    private String reviewPostUrl;
    private OffsetDateTime createdAt;
    private Long travelPlanId;
    private Long styleId;
}
