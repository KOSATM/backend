package com.example.demo.supporter.checklist.dao;

import org.apache.ibatis.annotations.Mapper;
import com.example.demo.supporter.checklist.dto.entity.ChecklistItem;
import java.util.List;

@Mapper
public interface ChecklistItemDao {
    ChecklistItem selectById(Long id);
    List<ChecklistItem> selectAll();
    int insert(ChecklistItem item);
    int update(ChecklistItem item);
    int delete(Long id);
}
