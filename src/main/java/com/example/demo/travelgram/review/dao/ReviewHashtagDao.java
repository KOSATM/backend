package com.example.demo.travelgram.review.dao;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.example.demo.travelgram.review.dto.entity.ReviewHashtag;
import com.example.demo.travelgram.review.dto.entity.ReviewHashtagGroup;

@Mapper
public interface ReviewHashtagDao {
    void deleteByPostId(Long postId);
    Long insertGroup(ReviewHashtagGroup group);
    void insertHashtag(ReviewHashtag hashtag);
    ReviewHashtagGroup findHashtagGroupByPostId(Long postId);
    List<ReviewHashtag> findHashtagsBygroupId(Long groupId);
    // 그룹아이디를 찾아서 그룹에 해당하는 해시태그를 포문으로 돌려야함

    
}
