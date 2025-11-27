package com.example.demo.supporter.imageSearch.dao;

import org.apache.ibatis.annotations.Mapper;
import com.example.demo.supporter.imageSearch.dto.entity.ImagePlace;
import java.util.List;

@Mapper
public interface ImagePlaceDao {
    ImagePlace selectById(Long id);
    List<ImagePlace> selectAll();
    int insert(ImagePlace place);
    int update(ImagePlace place);
    int deleteById(Long id);
}