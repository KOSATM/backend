package com.example.demo.travelgram.aiReview.dto.request;

import lombok.Data;


@Data
public class ReviewContentUpdateRequest {
    private Long postId;
    private String content;

}

// url은 Publish 할때 생김