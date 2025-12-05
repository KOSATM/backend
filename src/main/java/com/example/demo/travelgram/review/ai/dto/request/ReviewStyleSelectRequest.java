package com.example.demo.travelgram.review.ai.dto.request;

import lombok.Data;

@Data
public class ReviewStyleSelectRequest {
    private Long styleId;
    private Long analysisId;
    private Long postId;
}
