package com.example.demo.supporter.imageSearch.service;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import com.example.demo.supporter.imageSearch.dao.ImageSearchCandidateDao;
import com.example.demo.supporter.imageSearch.dto.entity.ImageSearchCandidate;

@Service
@RequiredArgsConstructor
public class ImageSearchCandidateService {
    private final ImageSearchCandidateDao dao;

    public ImageSearchCandidate get(Long id) {
        return dao.selectById(id);
    }

    public List<ImageSearchCandidate> getAll() {
        return dao.selectAll();
    }

    @Transactional
    public Long create(ImageSearchCandidate r) {
        dao.insert(r);
        return r.getId();
    }

    @Transactional
    public int update(ImageSearchCandidate r) {
        return dao.update(r);
    }

    @Transactional
    public int delete(Long id) {
        return dao.delete(id);
    }
}