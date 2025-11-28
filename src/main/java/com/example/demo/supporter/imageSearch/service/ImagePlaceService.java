package com.example.demo.supporter.imageSearch.service;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import com.example.demo.supporter.imageSearch.dao.ImagePlaceDao;
import com.example.demo.supporter.imageSearch.dto.entity.ImagePlace;

@Service
@RequiredArgsConstructor
public class ImagePlaceService {

    private final ImagePlaceDao dao;

    public ImagePlace get(Long id) {
        return dao.selectById(id);
    }

    public List<ImagePlace> getAll() {
        return dao.selectAll();
    }

    @Transactional
    public Long create(ImagePlace p) {
        dao.insert(p);
        return p.getId();
    }

    @Transactional
    public int update(ImagePlace p) {
        return dao.update(p);
    }

    @Transactional
    public int delete(Long id) {
        return dao.deleteById(id);
    }
}
