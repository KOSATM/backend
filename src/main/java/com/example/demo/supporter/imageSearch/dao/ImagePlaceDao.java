package com.example.demo.supporter.imageSearch.dao;

import org.apache.ibatis.annotations.Mapper;

import com.example.demo.supporter.imageSearch.dto.response.ImagePlaceResponse;

@Mapper
public interface ImagePlaceDao {
    int save(ImagePlaceResponse response);
    int deleteById(Long placeId);
    ImagePlaceResponse findByNameAndAddress(String name, String address);
    // ImagePlace selectById(Long id);
    // List<ImagePlace> selectAll();
    // int insert(ImagePlace place);
    // int update(ImagePlace place);
}