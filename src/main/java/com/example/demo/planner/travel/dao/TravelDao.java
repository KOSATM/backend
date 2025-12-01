package com.example.demo.planner.travel.dao;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.demo.planner.travel.dto.entity.TravelPlaces;
import com.example.demo.planner.travel.dto.response.TravelPlaceSearchResult;

@Mapper
public interface TravelDao {
    
    TravelPlaces findById(@Param("id") Long id);
    List<TravelPlaces> findAll(@Param("limit") int limit, @Param("offset") int offset);
    List<TravelPlaceSearchResult> searchByVector(@Param("embedding") float[] embedding, @Param("limit") int limit);
}
