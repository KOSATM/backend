package com.example.demo.travelgram.review.controller;

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

import com.example.demo.travelgram.review.dto.entity.ReviewPhoto;
import com.example.demo.travelgram.review.dto.entity.ReviewPost;
import com.example.demo.travelgram.review.dto.request.ReviewPhotoUploadRequest;
import com.example.demo.travelgram.review.dto.response.ReviewPhotoUploadResponse;
import com.example.demo.travelgram.review.service.ReviewPhotoService;
import com.example.demo.travelgram.review.service.ReviewPostService;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/review")
public class ReviewController {
    private final ReviewPhotoService reviewPhotoService;
    private final ReviewPostService reviewPostService;

    @PostMapping(value = "/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadReviewPhoto(
            @RequestPart("data") String data,
            @RequestPart("file") MultipartFile file) throws Exception {

        // JSON → DTO 변환
        ObjectMapper mapper = new ObjectMapper();
        ReviewPhotoUploadRequest dto = mapper.readValue(data, ReviewPhotoUploadRequest.class);

        // ✔ 서비스가 반환하는 타입에 맞춰서 호출
        ReviewPhotoUploadResponse response = reviewPhotoService.uploadPhoto(dto, file); 

        return ResponseEntity.ok(response);
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
