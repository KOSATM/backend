package com.example.demo.supporter.imageSearch.controller;

import com.example.demo.supporter.imageSearch.dto.entity.ImageSearchPlace;
import com.example.demo.supporter.imageSearch.service.ImageSearchPlaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/supporter/image-search/places")
@RequiredArgsConstructor
public class ImageSearchPlaceController {
    private final ImageSearchPlaceService service;

    @GetMapping("/{id}")
    public ResponseEntity<ImageSearchPlace> get(@PathVariable Long id) {
        return ResponseEntity.ok(service.get(id));
    }

    @GetMapping
    public ResponseEntity<List<ImageSearchPlace>> list() {
        return ResponseEntity.ok(service.getAll());
    }

    @PostMapping
    public ResponseEntity<Long> create(@RequestBody ImageSearchPlace p) {
        return ResponseEntity.ok(service.create(p));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Integer> update(@PathVariable Long id, @RequestBody ImageSearchPlace p) {
        p.setId(id);
        return ResponseEntity.ok(service.update(p));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Integer> delete(@PathVariable Long id) {
        return ResponseEntity.ok(service.delete(id));
    }
}