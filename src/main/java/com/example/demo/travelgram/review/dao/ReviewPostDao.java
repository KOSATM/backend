package com.example.demo.travelgram.review.dao;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.demo.travelgram.review.dto.entity.ReviewPhotoGroup;
import com.example.demo.travelgram.review.dto.entity.ReviewPost;

@Mapper
public interface ReviewPostDao {
    void insertDraft(ReviewPost post);

    ReviewPost findById(Long id);

    void update(ReviewPost post);
    
    void updateStyleId(@Param("id")Long id, @Param("styleId") Long styleId);
    
    // AI가 생성한 해시태그 Read해서 보여주기

    void updateContent(@Param("id") Long id, @Param("content") String content);


    void publish(@Param("id") Long id, @Param("postUrl") String postUrl);
    

    

    
}
