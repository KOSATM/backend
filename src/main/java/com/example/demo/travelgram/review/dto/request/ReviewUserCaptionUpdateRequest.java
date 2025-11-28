package com.example.demo.travelgram.review.dto.request;

import lombok.Data;


@Data
public class ReviewUserCaptionUpdateRequest {
    private Long postId;
    private String caption;

}

// url은 Publish 할때 생김