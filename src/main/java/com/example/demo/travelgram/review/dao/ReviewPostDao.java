package com.example.demo.travelgram.review.dao;

import org.apache.ibatis.annotations.Mapper;

import com.example.demo.travelgram.review.dto.entity.ReviewPost;

@Mapper
public interface ReviewPostDao {
    void insertDraft(ReviewPost post);



    

    

    
}
