package com.example.demo.supporter.imageSearch.service;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import com.example.demo.supporter.imageSearch.dao.ImageSearchResultDao;
import com.example.demo.supporter.imageSearch.dto.entity.ImageSearchResult;

@Service
@RequiredArgsConstructor
public class ImageSearchResultService {
    private final ImageSearchResultDao dao;

    public ImageSearchResult get(Long id) {
        return dao.selectById(id);
    }

    public List<ImageSearchResult> getAll() {
        return dao.selectAll();
    }

    @Transactional
    public Long create(ImageSearchResult r) {
        dao.insert(r);
        return r.getId();
    }

    @Transactional
    public int update(ImageSearchResult r) {
        return dao.update(r);
    }

    @Transactional
    public int delete(Long id) {
        return dao.deleteById(id);
    }
}