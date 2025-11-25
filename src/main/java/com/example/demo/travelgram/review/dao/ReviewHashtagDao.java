package com.example.demo.travelgram.review.dao;

import java.util.List;

import com.example.demo.travelgram.review.dto.entity.ReviewHashtag;
import com.example.demo.travelgram.review.dto.entity.ReviewHashtagGroup;

public interface ReviewHashtagDao {
    int insertHashtagGroup(ReviewHashtagGroup group);
    List<ReviewHashtagGroup> findHashtagGroupsByPostId(Long reviewPostId);

    int insertHashtag(ReviewHashtag hashtag);
    List<ReviewHashtag> findHashtagsByGroup(Long groupId);

    
}
