package com.example.demo.travelgram.review.dto.response;

import com.example.demo.travelgram.review.dto.entity.ReviewHashtagGroup;
import com.example.demo.travelgram.review.dto.entity.ReviewPhotoGroup;
import com.example.demo.travelgram.review.dto.entity.ReviewPost;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ReviewPostResponse {
    private ReviewPost post;
    private ReviewPhotoGroup photoGroup;
    private ReviewHashtagGroup hashtagGroup;


    // private List<ReviewPhoto> photos;
    // private List<ReviewHashtag> hashtags;
}