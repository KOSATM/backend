package com.example.demo.supporter.imageSearch.dao;

import org.apache.ibatis.annotations.Mapper;
import com.example.demo.supporter.imageSearch.dto.entity.ImageSearchCandidate;
import java.util.List;

@Mapper
public interface ImageSearchCandidateDao {
    ImageSearchCandidate selectById(Long id);
    List<ImageSearchCandidate> selectAll();
    int insert(ImageSearchCandidate r);
    int update(ImageSearchCandidate r);
    int delete(Long id);
}
