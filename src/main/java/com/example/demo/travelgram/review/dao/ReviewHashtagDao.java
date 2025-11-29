package com.example.demo.travelgram.review.dao;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.example.demo.travelgram.review.dto.entity.ReviewHashtag;
import com.example.demo.travelgram.review.dto.entity.ReviewHashtagGroup;

@Mapper
public interface ReviewHashtagDao {
    void insertHashtagGroup(Long postId);
    void insertHashtag(Long groupId);
    // void insertPersonalHashtag(Long groupId, String name); // request Dto로 받아야하나 근데 그럼 그냥 해시태그잔아
    void deleteHashtag(Long hashtagId);

    ReviewHashtagGroup findHashtagGroupByPostId(Long postId);
    List<ReviewHashtag> findHashtagsBygroupId(Long groupId); //읽기용

    
}
