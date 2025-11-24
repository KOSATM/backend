package com.example.demo.travelgram.dao.reviewPhoto;

import org.apache.ibatis.annotations.Mapper;

import com.example.demo.dto.reviewPhoto.ReviewPhotoGroup;

@Mapper
public interface ReviewPhotoGroupDao {
    int insertReviewPhotoGroup(ReviewPhotoGroup reviewPhotoGroup);
    
}
