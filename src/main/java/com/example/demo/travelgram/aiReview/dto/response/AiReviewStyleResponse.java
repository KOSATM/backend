package com.example.demo.travelgram.aiReview.dto.response;

import java.util.List;

import com.example.demo.travelgram.aiReview.dto.entity.AiReviewStyle;
import com.example.demo.travelgram.review.dto.entity.ReviewHashtag;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AiReviewStyleResponse {
    private AiReviewStyle style;
    private String content;
    private List<ReviewHashtag> hashtags;
}
