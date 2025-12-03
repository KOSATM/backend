package com.example.demo.common.travel.dao;

import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TravelPlaceDao {
    String selectImgUrlByTitle(String title);
}
