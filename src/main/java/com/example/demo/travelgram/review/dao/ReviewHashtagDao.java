package com.example.demo.travelgram.review.dao;

import org.apache.ibatis.annotations.Mapper;

import com.example.demo.travelgram.review.dto.entity.ReviewHashtag;
import com.example.demo.travelgram.review.dto.entity.ReviewHashtagGroup;


@Mapper
public interface ReviewHashtagDao {
    void insertHashtagGroup(ReviewHashtagGroup group);
    void insertHashtag(ReviewHashtag hashtag);


    
}
