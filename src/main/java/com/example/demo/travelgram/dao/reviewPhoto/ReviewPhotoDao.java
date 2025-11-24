package com.example.demo.travelgram.dao.reviewPhoto;

import org.apache.ibatis.annotations.Mapper;

import com.example.demo.dto.reviewPhoto.ReviewPhoto;

@Mapper
public interface ReviewPhotoDao {
    int insertReviewPhoto(ReviewPhoto reviewPhoto);
    
}
