package com.example.demo.travelgram.aiReview.dao;

import com.example.demo.travelgram.aiReview.dto.entity.*;
public interface AiReviewDao {
    void insertAiReview(AiReviewAnalysis aiReviewAnalysis);
    void insertAiReviewHashtag(AiReviewHashtag aiReviewHashtag);
    void insertAiReviewStyle(AiReviewStyle aiReviewStyle);
    
}
