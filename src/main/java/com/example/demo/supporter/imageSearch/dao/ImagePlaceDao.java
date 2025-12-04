package com.example.demo.supporter.imageSearch.dao;

import org.apache.ibatis.annotations.Mapper;

import com.example.demo.supporter.imageSearch.dto.response.ImagePlaceResponse;

@Mapper
public interface ImagePlaceDao {
    ImagePlaceResponse save(ImagePlaceResponse response);
    // ImagePlace selectById(Long id);
    // List<ImagePlace> selectAll();
    // int insert(ImagePlace place);
    // int update(ImagePlace place);
    // int deleteById(Long id);
}