package com.example.demo.travelgram.review.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ReviewPostResponse {
    private Long id;
    private String postUrl;
    // private boolean isPosted = true;
}