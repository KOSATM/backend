package com.example.demo.travelgram.review.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.travelgram.review.dto.entity.ReviewPost;
import com.example.demo.travelgram.review.dto.request.ReviewPhotoUploadRequest;
import com.example.demo.travelgram.review.dto.response.ReviewPhotoUploadResponse;
import com.example.demo.travelgram.review.service.ReviewPhotoService;
import com.example.demo.travelgram.review.service.ReviewPostService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/review")
public class ReviewController {
    private final ReviewPhotoService reviewPhotoService;
    private final ReviewPostService reviewPostService;

    @PostMapping(value = "/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadReviewPhotos(
            @RequestPart("dataList") List<ReviewPhotoUploadRequest> requests,
            @RequestPart("files") List<MultipartFile> files) {

        List<ReviewPhotoUploadResponse> result = new ArrayList<>();

        for (int i = 0; i < files.size(); i++) {
            ReviewPhotoUploadResponse res = reviewPhotoService.uploadPhoto(
                    requests.get(i),
                    files.get(i));
            result.add(res);
        }

        return ResponseEntity.ok(result);
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
