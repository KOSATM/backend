package com.example.demo.supporter.imageSearch.dao;

import org.apache.ibatis.annotations.Mapper;
import com.example.demo.supporter.imageSearch.dto.entity.ImageSearchResult;
import java.util.List;

@Mapper
public interface ImageSearchResultDao {
    ImageSearchResult selectById(Long id);
    List<ImageSearchResult> selectAll();
    int insert(ImageSearchResult r);
    int update(ImageSearchResult r);
    int delete(Long id);
}
