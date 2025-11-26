package com.example.demo.travelgram.review.dao;

import org.apache.ibatis.annotations.Mapper;

import com.example.demo.travelgram.review.dto.entity.ReviewPhoto;
import com.example.demo.travelgram.review.dto.entity.ReviewPhotoGroup;


@Mapper
public interface ReviewPhotoDao {

    void insertReviewPhotoGroup(ReviewPhotoGroup group);
    void insertReviewPhoto(ReviewPhoto photo);
    // List<ReviewPhoto> findPhotosByGroup(Long groupId);
}