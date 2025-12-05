package com.example.demo.travelgram.review.dao;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.demo.travelgram.review.dto.entity.ReviewPhoto;
import com.example.demo.travelgram.review.dto.entity.ReviewPhotoGroup;


@Mapper
public interface ReviewPhotoDao {

    void insertReviewPhotoGroup(ReviewPhotoGroup group);
    void insertReviewPhoto(ReviewPhoto reviewPhoto);
    void updatePhotoOrder(@Param("photoId") Long photoId,
                      @Param("orderIndex") Integer orderIndex,
                      @Param("photoGroupId") Long photoGroupId);
    ReviewPhoto selectReviewPhotoById(Long id);
    ReviewPhotoGroup selectPhotoGroupByPostId(Long postId);
    List<ReviewPhoto> selectReviewPhotosByPhotoGroupId(Long photoGroupId);
    List<String> selectPhotoSummariesByPhotoGroupId(Long photoGroupId);


    void deleteReviewPhoto(Long id);
}