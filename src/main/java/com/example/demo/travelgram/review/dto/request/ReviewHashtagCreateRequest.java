package com.example.demo.travelgram.review.dto.request;

import lombok.Data;

@Data
public class ReviewHashtagCreateRequest {
    private Long postId;
    private String name;
    
}
