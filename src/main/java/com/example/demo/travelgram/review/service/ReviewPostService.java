package com.example.demo.travelgram.review.service;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.example.demo.travelgram.review.dao.ReviewPostDao;
import com.example.demo.travelgram.review.dto.entity.*;
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
}
