package com.example.demo.travelgram.review.dao;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import com.example.demo.travelgram.review.dto.entity.*;

@Mapper
public interface ReviewPostDao {
    void insertDraft(ReviewPost post);
    void updateReviewPost(ReviewPost post);
    ReviewPost findReviewPostById(Long id);
    List<ReviewPost> findReviewPostsByPlanId(Long travelPlanId);

    List<ReviewPhotoGroup> findGroupsByPostId(Long reviewPostId);

    

    
}
