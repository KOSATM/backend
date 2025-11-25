package com.example.demo.supporter.map.service;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import com.example.demo.supporter.map.dao.ToiletDao;
import com.example.demo.supporter.map.dto.entity.Toilet;

@Service
@RequiredArgsConstructor
public class ToiletService {
    private final ToiletDao dao;

    public Toilet get(Long id) {
        return dao.selectById(id);
    }

    public List<Toilet> getAll() {
        return dao.selectAll();
    }

    @Transactional
    public Long create(Toilet t) {
        dao.insert(t);
        return t.getId();
    }

    @Transactional
    public int update(Toilet t) {
        return dao.update(t);
    }

    @Transactional
    public int delete(Long id) {
        return dao.deleteById(id);
    }
}