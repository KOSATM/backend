package com.example.demo.supporter.imageSearch.dao;

import org.apache.ibatis.annotations.Mapper;

import com.example.demo.supporter.imageSearch.dto.entity.ImageSearchSession;

import java.util.List;

@Mapper
public interface ImageSearchSessionDao {
    ImageSearchSession selectById(Long id);
    List<ImageSearchSession> selectAll();
    int insert(ImageSearchSession place);
    int update(ImageSearchSession place);
    int delete(Long id);
}
