package com.example.demo.supporter.imageSearch.dao;

import org.apache.ibatis.annotations.Mapper;
import com.example.demo.supporter.imageSearch.dto.entity.ImageSearchPlace;
import java.util.List;

@Mapper
public interface ImageSearchPlaceDao {
    ImageSearchPlace selectById(Long id);
    List<ImageSearchPlace> selectAll();
    int insert(ImageSearchPlace place);
    int update(ImageSearchPlace place);
    int delete(Long id);
}
