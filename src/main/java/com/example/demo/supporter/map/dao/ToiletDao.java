package com.example.demo.supporter.map.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import com.example.demo.supporter.map.dto.entity.Toilet;
import java.util.List;

@Mapper
public interface ToiletDao {
    int insert(Toilet toilet);
    int insertBatch(List<Toilet> toilets);
    int deleteAll();
    List<Toilet> findInBounds(
        @Param("northEastLat") Double northEastLat, 
        @Param("northEastLng") Double northEastLng, 
        @Param("southWestLat") Double southWestLat, 
        @Param("southWestLng") Double southWestLng
    );
    List<Toilet> findNearest(
        @Param("userLat") Double userLat, 
        @Param("userLng") Double userLng, 
        @Param("limit") Integer limit
    );
}
