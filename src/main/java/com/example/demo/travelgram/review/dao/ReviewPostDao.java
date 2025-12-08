package com.example.demo.travelgram.review.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.demo.travelgram.review.dto.entity.ReviewPost;

@Mapper
public interface ReviewPostDao {
    void insertDraft(ReviewPost post);
    void updateReviewPostGroupId(
        @Param("reviewPostId")Long reviewPostId, 
        @Param("photoGroupId")Long photoGroupId, 
        @Param("hashtagGroupId") Long hashtagGroupId);
    void updateReviewPostMood(
        @Param("photoGroupId") Long photoGroupId, 
        @Param("overallMoods") String overallMoods, 
        @Param("travelType") String travelType);
    ReviewPost selectReviewPostById(Long reviewPostId);
    void updateReviewPostStyleIdById(
        @Param("reviewPostId") Long reviewPostId,
        @Param("reviewStyleId")Long reviewStyleId);
    void updateReviewPostCaptionIdById(
        @Param("reviewPostId") Long reviewPostId,
        @Param("caption") String caption);
    

    
}
