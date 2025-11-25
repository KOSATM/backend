package com.example.demo.supporter.imageSearch.service;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import com.example.demo.supporter.imageSearch.dao.PlaceDao;
import com.example.demo.supporter.imageSearch.dto.entity.Place;

@Service
@RequiredArgsConstructor
public class PlaceService {

    private final PlaceDao dao;

    public Place get(Long id) {
        return dao.selectById(id);
    }

    public List<Place> getAll() {
        return dao.selectAll();
    }

    @Transactional
    public Long create(Place p) {
        dao.insert(p);
        return p.getId();
    }

    @Transactional
    public int update(Place p) {
        return dao.update(p);
    }

    @Transactional
    public int delete(Long id) {
        return dao.deleteById(id);
    }
}
