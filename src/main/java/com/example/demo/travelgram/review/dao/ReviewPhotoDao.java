package com.example.demo.travelgram.review.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.demo.travelgram.review.dto.entity.ReviewPhoto;
import com.example.demo.travelgram.review.dto.entity.ReviewPhotoGroup;


@Mapper
public interface ReviewPhotoDao {

    void insertReviewPhotoGroup(ReviewPhotoGroup group);
    void insertReviewPhoto(ReviewPhoto photo);
    void updatePhotoOrder(@Param("photoId") Long photoId,
                      @Param("orderIndex") Integer orderIndex,
                      @Param("groupId") Long groupId);
    // List<ReviewPhoto> findPhotosByGroup(Long groupId);
    void deleteReviewPhoto(Long id);
}