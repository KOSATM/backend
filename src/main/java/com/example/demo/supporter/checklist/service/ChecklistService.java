package com.example.demo.supporter.checklist.service;

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

    public List<Checklist> getAll() {
        return dao.selectAllChecklists();
    }

    @Transactional
    public Long create(Checklist c) {
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
}