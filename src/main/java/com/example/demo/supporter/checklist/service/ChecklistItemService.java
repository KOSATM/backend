package com.example.demo.supporter.checklist.service;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import com.example.demo.supporter.checklist.dao.ChecklistItemDao;
import com.example.demo.supporter.checklist.dto.entity.ChecklistItem;

@Service
@RequiredArgsConstructor
public class ChecklistItemService {
    private final ChecklistItemDao dao;

    public ChecklistItem get(Long id) {
        return dao.selectById(id);
    }

    public List<ChecklistItem> getAll() {
        return dao.selectAll();
    }

    @Transactional
    public Long create(ChecklistItem item) {
        dao.insert(item);
        return item.getId();
    }

    @Transactional
    public int update(ChecklistItem item) {
        return dao.update(item);
    }

    @Transactional
    public int delete(Long id) {
        return dao.deleteById(id);
    }
}