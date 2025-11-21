package com.example.demo.travelgram.dao.reviewHashtag;

import org.apache.ibatis.annotations.Mapper;

import com.example.demo.dto.reviewHashtag.ReviewHashtagGroup;

@Mapper
public interface ReviewHashtagGroupDao {
    int insertReviewHashtagGroup(ReviewHashtagGroup reviewHashtagGroup);
}