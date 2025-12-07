package com.example.demo.supporter.imageSearch.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.demo.supporter.imageSearch.dto.response.ImageSearchSessionResponse;

@Mapper
public interface ImageSearchSessionDao {
    int insert(ImageSearchSessionResponse session);
    ImageSearchSessionResponse selectById(Long id);
    java.util.List<ImageSearchSessionResponse> selectByUserId(Long userId);
    int updateActionType(@Param("sessionId") Long sessionId, @Param("actionType") String actionType);
    int delete(Long id);
    ImageSearchSessionResponse findRecentSessionByUserAndPlaces(
        @Param("userId") Long userId, 
        @Param("placeIds") java.util.List<Long> placeIds
    );
    // List<ImageSearchSession> selectAll();
    // int update(ImageSearchSession place);
}
