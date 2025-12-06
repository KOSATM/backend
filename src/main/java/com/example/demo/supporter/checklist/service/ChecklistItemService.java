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
        return dao.selectChecklistItemById(id);
    }

    public List<ChecklistItem> getAll(Long checklistId) {
        return dao.selectChecklistItemsByChecklistId(checklistId);
    }

    @Transactional
    public Long create(ChecklistItem item) {
        dao.insertChecklistItem(item);
        return item.getId();
    }

    @Transactional
    public int update(ChecklistItem item) {
        return dao.updateChecklistItem(item);
    }

    @Transactional
    public int delete(Long id) {
        return dao.deleteChecklistItem(id);
    }

    // 체크리스트 항목 전부 삭
    @Transactional
    public int deleteItemsByChecklistId(Long checklistId) {
        return dao.deleteChecklistItemsByChecklistId(checklistId);
    }
}