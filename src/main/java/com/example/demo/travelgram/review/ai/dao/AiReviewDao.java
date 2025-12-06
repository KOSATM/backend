package com.example.demo.travelgram.review.ai.dao;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.example.demo.travelgram.review.ai.dto.entity.AiReviewAnalysis;
import com.example.demo.travelgram.review.ai.dto.entity.AiReviewHashtag;
import com.example.demo.travelgram.review.ai.dto.entity.AiReviewStyle;
@Mapper
public interface AiReviewDao {

    void insertAiReviewAnalysis(AiReviewAnalysis analysis);
    void insertAiReviewStyle(AiReviewStyle style);
    void insertAiReviewHashtag(AiReviewHashtag tag);
    List<AiReviewStyle> selectAllStylesByAnalysisId(Long reviewAnalysisId);
    List<AiReviewHashtag> selectHashtagsByStyleId(Long styleId);
}

