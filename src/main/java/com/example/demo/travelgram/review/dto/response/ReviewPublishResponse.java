package com.example.demo.travelgram.review.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class ReviewPublishResponse {
    private boolean isPosted = true;
    private String postUrl;
}
