package com.example.demo.travelgram.review.dao;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.example.demo.travelgram.review.dto.entity.ReviewHashtag;
import com.example.demo.travelgram.review.dto.entity.ReviewHashtagGroup;

@Mapper
public interface ReviewHashtagDao {
    void deleteByPostId(Long postId);
    Long insertGroup(ReviewHashtagGroup group);
    void insertHashtag(ReviewHashtag hashtag);
    List<String> findHashtagsByPostId(Long postId);

    
}
