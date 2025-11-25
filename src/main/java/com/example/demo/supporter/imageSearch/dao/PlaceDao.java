package com.example.demo.supporter.imageSearch.dao;

import org.apache.ibatis.annotations.Mapper;
import com.example.demo.supporter.imageSearch.dto.entity.Place;
import java.util.List;

@Mapper
public interface PlaceDao {
    Place selectById(Long id);
    List<Place> selectAll();
    int insert(Place place);
    int update(Place place);
    int deleteById(Long id);
}