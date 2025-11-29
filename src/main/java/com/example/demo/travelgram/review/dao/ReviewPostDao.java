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

    void updateUserCaption(@Param("id") Long id, @Param("cation") String caption);


    void publish(@Param("id") Long id, @Param("postUrl") String postUrl);
    

    

    
}
