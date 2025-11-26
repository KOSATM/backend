package com.example.demo.supporter.imageSearch.service;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import com.example.demo.supporter.imageSearch.dao.ImageSearchPlaceDao;
import com.example.demo.supporter.imageSearch.dto.entity.ImageSearchPlace;

@Service
@RequiredArgsConstructor
public class ImageSearchPlaceService {
    private final ImageSearchPlaceDao dao;

    public ImageSearchPlace get(Long id) {
        return dao.selectById(id);
    }

    public List<ImageSearchPlace> getAll() {
        return dao.selectAll();
    }

    @Transactional
    public Long create(ImageSearchPlace p) {
        dao.insert(p);
        return p.getId();
    }

    @Transactional
    public int update(ImageSearchPlace p) {
        return dao.update(p);
    }

    @Transactional
    public int delete(Long id) {
        return dao.delete(id);
    }
}