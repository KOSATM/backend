package com.example.demo.travelgram.review.service;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.example.demo.travelgram.review.dao.ReviewPostDao;
import com.example.demo.travelgram.review.dto.*;
import com.example.demo.travelgram.review.dto.entity.ReviewHashtag;
import com.example.demo.travelgram.review.dto.entity.ReviewHashtagGroup;
import com.example.demo.travelgram.review.dto.entity.ReviewPhoto;
import com.example.demo.travelgram.review.dto.entity.ReviewPhotoGroup;
import com.example.demo.travelgram.review.dto.entity.ReviewPost;

@Service
public class ReviewPostService {

    @Autowired
    private ReviewPostDao dao;

    public Long createReviewPost(ReviewPost post) {
        dao.insertReviewPost(post);
        return post.getId();
    }

    public List<ReviewPost> getReviewPostsByPlan(Long planId) {
        return dao.findReviewPostsByPlanId(planId);
    }

    public Long addPhotoGroup(ReviewPhotoGroup group) {
        dao.insertPhotoGroup(group);
        return group.getId();
    }

    public Long addPhoto(ReviewPhoto photo) {
        dao.insertPhoto(photo);
        return photo.getId();
    }

    public Long addHashtagGroup(ReviewHashtagGroup group) {
        dao.insertHashtagGroup(group);
        return group.getId();
    }

    public Long addHashtag(ReviewHashtag hashtag) {
        dao.insertHashtag(hashtag);
        return hashtag.getId();
    }
}
