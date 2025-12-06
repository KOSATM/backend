package com.example.demo.travelgram.review.ai.dto.response;

import java.util.List;

import com.example.demo.travelgram.review.ai.dto.entity.AiReviewHashtag;
import com.example.demo.travelgram.review.ai.dto.entity.AiReviewStyle;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AiReviewStyleResponse {
    private AiReviewStyle style; //caption 포함 되어있음
    private List<AiReviewHashtag> hashtags;
}
