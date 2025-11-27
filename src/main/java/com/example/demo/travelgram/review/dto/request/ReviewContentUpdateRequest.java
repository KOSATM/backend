package com.example.demo.travelgram.review.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class ReviewContentUpdateRequest {
    private Long id; //review_posts
    private String content;
    private Long review_style_id;
    
}
