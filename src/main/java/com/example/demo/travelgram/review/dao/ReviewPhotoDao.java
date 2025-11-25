package com.example.demo.travelgram.review.dao;

import java.util.List;

import com.example.demo.travelgram.review.dto.entity.ReviewPhoto;
import com.example.demo.travelgram.review.dto.entity.ReviewPhotoGroup;

public interface ReviewPhotoDao {

    void insert(ReviewPhoto photo);
    
    // int insertPhotoGroup(ReviewPhotoGroup group);
    // int insertPhoto(ReviewPhoto photo);
    // List<ReviewPhoto> findPhotosByGroup(Long groupId);
}