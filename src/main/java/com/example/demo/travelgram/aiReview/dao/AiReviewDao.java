package com.example.demo.travelgram.aiReview.dao;

import org.apache.ibatis.annotations.Mapper;

import com.example.demo.travelgram.aiReview.dto.entity.AiReviewAnalysis;
import com.example.demo.travelgram.aiReview.dto.entity.AiReviewHashtag;
import com.example.demo.travelgram.aiReview.dto.entity.AiReviewStyle;
@Mapper
public interface AiReviewDao {

    void insertAiReview(AiReviewAnalysis analysis);
    void insertAiReviewStyle(AiReviewStyle style);
    void insertAiReviewHashtag(AiReviewHashtag tag);
    AiReviewStyle findStyleById(Long id);
}

