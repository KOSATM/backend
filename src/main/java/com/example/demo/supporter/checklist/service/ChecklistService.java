package com.example.demo.supporter.checklist.service;

import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import com.example.demo.supporter.checklist.dao.ChecklistDao;
import com.example.demo.supporter.checklist.dto.entity.Checklist;

@Service
@RequiredArgsConstructor
public class ChecklistService {
    private final ChecklistDao dao;

    public Checklist get(Long id) {
        return dao.selectChecklistById(id);
    }

    public List<Checklist> getAll(Long userId) {
        return dao.selectChecklistsByUserId(userId);
    }

    @Transactional
    public Long create(Checklist c) {
        c.setCreatedAt(OffsetDateTime.now());
        dao.insertChecklist(c);
        return c.getId();
    }

    @Transactional
    public int update(Checklist c) {
        return dao.updateChecklist(c);
    }

    @Transactional
    public int delete(Long id) {
        return dao.deleteChecklist(id);
    }

    // 체크리스트 삭제하는 메서드
    @Transactional
    public int deleteItemsByUserId(Long userId) {
        return dao.deleteChecklistsByUserId(userId);
    }

    public List<Checklist> findByUserId(Long userId) {
    return dao.selectChecklistsByUserId(userId);
}
}