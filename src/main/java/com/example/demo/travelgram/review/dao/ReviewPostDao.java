package com.example.demo.travelgram.review.dao;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import com.example.demo.travelgram.review.dto.entity.*;

@Mapper
public interface ReviewPostDao {
    // travelPlanId 기반 review_post 생성
    void insertDraft(ReviewPost post);
    // 리뷰 내용 수정/저장
    void updateReviewPost(ReviewPost post);
    ReviewPost findReviewPostById(Long id);
    List<ReviewPost> findReviewPostsByPlanId(Long travelPlanId);

    List<ReviewPhotoGroup> findGroupsByPostId(Long reviewPostId);

    

    
}
