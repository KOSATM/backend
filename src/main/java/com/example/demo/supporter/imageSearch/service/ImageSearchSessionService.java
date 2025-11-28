package com.example.demo.supporter.imageSearch.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.supporter.imageSearch.dao.ImageSearchSessionDao;
import com.example.demo.supporter.imageSearch.dto.entity.ImageSearchSession;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ImageSearchSessionService {
    private final ImageSearchSessionDao dao;
    
    public ImageSearchSession get(Long id) {
        return dao.selectById(id);
    }

    public List<ImageSearchSession> getAll() {
        return dao.selectAll();
    }

    @Transactional
    public Long create(ImageSearchSession p) {
        dao.insert(p);
        return p.getId();
    }

    @Transactional
    public int update(ImageSearchSession p) {
        return dao.update(p);
    }

    @Transactional
    public int delete(Long id) {
        return dao.delete(id);
    }
}