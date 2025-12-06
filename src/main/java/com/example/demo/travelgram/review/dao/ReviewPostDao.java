package com.example.demo.travelgram.review.dao;

import org.apache.ibatis.annotations.Mapper;

import com.example.demo.travelgram.review.dto.entity.ReviewPost;

@Mapper
public interface ReviewPostDao {
    void insertDraft(ReviewPost post);
    void updateReviewPostGroupId(Long reviewPostId, Long photoGroupId, Long hashtagGroupId);
    void updateReviewPostMood(Long photoGroupId, String overallMoods, String travelType);
    ReviewPost selectReviewPostById(Long reviewPostId);
    void updateReviewPostStyleIdById(Long reviewPostId, Long reviewStyleId);

    

    
}
