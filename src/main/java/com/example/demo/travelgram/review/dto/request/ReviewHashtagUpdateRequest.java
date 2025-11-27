package com.example.demo.travelgram.review.dto.request;

import lombok.Data;

@Data
public class ReviewHashtagUpdateRequest {
    private Long reviewPostId;
    private String hashtagName;
    
}
