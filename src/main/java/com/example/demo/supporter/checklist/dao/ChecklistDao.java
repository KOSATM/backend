package com.example.demo.supporter.checklist.dao;

import org.apache.ibatis.annotations.Mapper;
import com.example.demo.supporter.checklist.dto.entity.Checklist;
import java.util.List;

@Mapper
public interface ChecklistDao {
    Checklist selectById(Long id); // select
    List<Checklist> selectAll();  // select all
    int insert(Checklist checklist); // insert
    int update(Checklist checklist); // update
    int delete(Long id); // delete
}
