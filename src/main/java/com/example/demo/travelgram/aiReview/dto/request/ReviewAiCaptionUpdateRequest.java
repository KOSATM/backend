package com.example.demo.travelgram.aiReview.dto.request;

import lombok.Data;


@Data
public class ReviewAiCaptionUpdateRequest {
    private Long postId;
    private String caption;

}

// url은 Publish 할때 생김