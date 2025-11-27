package com.example.demo.travelgram.review.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ReviewPublishResponse {

    private Long id; //review_posts
    private String postUrl;
    private boolean is_posted = true;
}
