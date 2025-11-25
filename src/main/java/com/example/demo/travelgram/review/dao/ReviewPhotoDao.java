package com.example.demo.travelgram.review.dao;

import org.apache.ibatis.annotations.Mapper;

import com.example.demo.travelgram.review.dto.entity.ReviewPhoto;


@Mapper
public interface ReviewPhotoDao {

    void insert(ReviewPhoto photo);
    
    // int insertPhotoGroup(ReviewPhotoGroup group);
    // int insertPhoto(ReviewPhoto photo);
    // List<ReviewPhoto> findPhotosByGroup(Long groupId);
}