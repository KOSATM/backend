package com.example.demo.travelgram.review.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.travelgram.review.dto.entity.ReviewPost;
import com.example.demo.travelgram.review.dto.request.PhotoUploadRequest;
import com.example.demo.travelgram.review.dto.response.PhotoUploadResponse;
import com.example.demo.travelgram.review.service.ReviewPhotoService;
import com.example.demo.travelgram.review.service.ReviewPostService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/review")
public class ReviewController {
    private final ReviewPhotoService reviewPhotoService;
    private final ReviewPostService reviewPostService;

    @PostMapping("/photo/upload")
    public PhotoUploadResponse uploadPhoto(@RequestPart("data") PhotoUploadRequest request,
            @RequestPart("file") MultipartFile file){
        return reviewPhotoService.uploadPhoto(request, file);
    }
    
    @PostMapping("/post")
    public Long createReviewPost(@RequestBody ReviewPost post) {
        return reviewPostService.createReviewPost(post);
    }

    @GetMapping("/posts/{planId}")
    public List<ReviewPost> getReviewPosts(@PathVariable Long planId) {
        return reviewPostService.getReviewPostsByPlan(planId);
    }
}
