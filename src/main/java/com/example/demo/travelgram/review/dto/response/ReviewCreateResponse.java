package com.example.demo.travelgram.review.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class ReviewCreateResponse {
    private Long reviewPostId;
    private Long photoGroupId;
    private Long hashtagGroupId;
}
