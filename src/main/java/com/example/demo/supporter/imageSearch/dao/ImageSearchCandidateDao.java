package com.example.demo.supporter.imageSearch.dao;

import org.apache.ibatis.annotations.Mapper;

import com.example.demo.supporter.imageSearch.dto.entity.ImageSearchCandidate;
import com.example.demo.supporter.imageSearch.dto.response.ImageSearchCandidateResponse;

@Mapper
public interface ImageSearchCandidateDao {
    int insert(ImageSearchCandidateResponse candidate);
    ImageSearchCandidate selectById(Long id);
    java.util.List<ImageSearchCandidate> selectBySessionId(Long sessionId);
    java.util.List<com.example.demo.supporter.imageSearch.dto.response.CandidateWithPlaceResponse> selectWithPlaceBySessionId(Long sessionId);
    int delete(Long id);
    int deleteBySessionId(Long sessionId);
    // List<ImageSearchCandidate> selectAll();
    // int insert(ImageSearchCandidate r);
}
