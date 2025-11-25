package com.example.demo.travelgram.review.controller;

import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.example.demo.travelgram.review.dto.entity.*;
import com.example.demo.travelgram.review.service.ReviewPostService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/review")
public class ReviewPostController {

    
    private final ReviewPostService reviewPostService;

    @PostMapping("/post")
    public Long createReviewPost(@RequestBody ReviewPost post) {
        return reviewPostService.createReviewPost(post);
    }

    @GetMapping("/posts/{planId}")
    public List<ReviewPost> getReviewPosts(@PathVariable Long planId) {
        return reviewPostService.getReviewPostsByPlan(planId);
    }

    @PostMapping("/photo/group")
    public Long addPhotoGroup(@RequestBody ReviewPhotoGroup group) {
        return reviewPostService.addPhotoGroup(group);
    }

    @PostMapping("/photo")
    public Long addPhoto(@RequestBody ReviewPhoto photo) {
        return reviewPostService.addPhoto(photo);
    }

    @PostMapping("/hashtag/group")
    public Long addHashtagGroup(@RequestBody ReviewHashtagGroup group) {
        return reviewPostService.addHashtagGroup(group);
    }

    @PostMapping("/hashtag")
    public Long addHashtag(@RequestBody ReviewHashtag hashtag) {
        return reviewPostService.addHashtag(hashtag);
    }
}
