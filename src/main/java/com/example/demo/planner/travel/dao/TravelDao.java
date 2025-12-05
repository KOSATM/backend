package com.example.demo.planner.travel.dao;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.demo.planner.travel.dto.TravelPlaceCandidate;
import com.example.demo.planner.travel.dto.entity.TravelPlaces;

@Mapper
public interface TravelDao {
    
    TravelPlaces findById(@Param("id") Long id);
    List<TravelPlaces> findAll(@Param("limit") int limit, @Param("offset") int offset);
    List<TravelPlaceCandidate> searchByVector(@Param("embedding") float[] embedding, @Param("limit") int limit);
    List<TravelPlaceCandidate> searchMissingCategoryByVector(Map<String, Object> params);
}
