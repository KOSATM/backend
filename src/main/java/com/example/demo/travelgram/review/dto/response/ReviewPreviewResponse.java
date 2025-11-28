package com.example.demo.travelgram.review.dto.response;

import java.util.List;

import com.example.demo.travelgram.review.dto.entity.ReviewHashtag;
import com.example.demo.travelgram.review.dto.entity.ReviewPhoto;
import com.example.demo.travelgram.review.dto.entity.ReviewPost;

import lombok.AllArgsConstructor;
import lombok.Data;
@AllArgsConstructor
@Data
public class ReviewPreviewResponse {
    private ReviewPost post;
    private List<ReviewPhoto> photos;
    private List<ReviewHashtag> hashtags;

}
