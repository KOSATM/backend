package com.example.demo.travelgram.dao.reviewHashtag;

import org.apache.ibatis.annotations.Mapper;

import com.example.demo.dto.reviewHashtag.ReviewHashtag;

@Mapper
public interface ReviewHashtagDao {
    int insertReviewHashtag(ReviewHashtag reviewHashtag);
    
}
