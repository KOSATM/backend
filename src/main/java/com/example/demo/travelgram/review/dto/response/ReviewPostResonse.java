package com.example.demo.travelgram.review.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ReviewPostResonse {
    private Long id; //review_posts
    private String postUrl;
}
