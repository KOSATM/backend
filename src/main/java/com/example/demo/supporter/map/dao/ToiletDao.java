package com.example.demo.supporter.map.dao;

import org.apache.ibatis.annotations.Mapper;
import com.example.demo.supporter.map.dto.entity.Toilet;
import java.util.List;

@Mapper
public interface ToiletDao {
    Toilet selectById(Long id);
    List<Toilet> selectAll();
    int insert(Toilet toilet);
    int update(Toilet toilet);
    int deleteById(Long id);
}
