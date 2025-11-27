package com.example.demo.travelgram.aiReview.agent;

import com.example.demo.travelgram.aiReview.dto.response.AiReviewStyleResponse;

public interface AiReviewAgent {
    AiReviewStyleResponse generateAiStyles(Long postId);
    // Style Response에 content, hashtag는 포함.
    // 그렇다면 aiReviewHashtagGroup도 필요한 거 아닌가?

    

}
