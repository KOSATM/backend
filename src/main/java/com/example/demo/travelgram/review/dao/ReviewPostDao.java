package com.example.demo.travelgram.review.dao;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import com.example.demo.travelgram.review.dto.*;
import com.example.demo.travelgram.review.dto.entity.ReviewHashtag;
import com.example.demo.travelgram.review.dto.entity.ReviewHashtagGroup;
import com.example.demo.travelgram.review.dto.entity.ReviewPhoto;
import com.example.demo.travelgram.review.dto.entity.ReviewPhotoGroup;
import com.example.demo.travelgram.review.dto.entity.ReviewPost;

@Mapper
public interface ReviewPostDao {
    int insertReviewPost(ReviewPost post);
    ReviewPost findReviewPostById(Long id);
    List<ReviewPost> findReviewPostsByPlanId(Long travelPlanId);

    int insertPhotoGroup(ReviewPhotoGroup group);
    List<ReviewPhotoGroup> findGroupsByPostId(Long reviewPostId);

    int insertPhoto(ReviewPhoto photo);
    List<ReviewPhoto> findPhotosByGroup(Long groupId);

    int insertHashtagGroup(ReviewHashtagGroup group);
    List<ReviewHashtagGroup> findHashtagGroupsByPostId(Long reviewPostId);

    int insertHashtag(ReviewHashtag hashtag);
    List<ReviewHashtag> findHashtagsByGroup(Long groupId);
}
