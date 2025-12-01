package com.example.demo.supporter.map.dao;

import org.apache.ibatis.annotations.Mapper;
import com.example.demo.supporter.map.dto.entity.Toilet;
import java.util.List;

@Mapper
public interface ToiletDao {
    int insert(Toilet toilet);
    int insertBatch(List<Toilet> toilets);
    int deleteAll();
    List<Toilet> findInBounds(Double northEastLat, Double northEastLng, Double southWestLat, Double southWestLng);
    List<Toilet> findNearest(Double userLat, Double userLng, Integer limit);
}
