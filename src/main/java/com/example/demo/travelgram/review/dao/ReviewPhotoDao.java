package com.example.demo.travelgram.review.dao;

import java.util.List;

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
    ReviewPhotoGroup findPhotoGroupByPostId(Long postId);
    // List<ReviewPhoto> findPhotosByGroup(Long groupId);
    List<ReviewPhoto> findByPostId(Long postId);
    void deleteReviewPhoto(Long id);
}