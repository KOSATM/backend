package com.example.demo.supporter.imageSearch.controller;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.example.demo.supporter.imageSearch.dto.entity.ImageSearchResult;
import com.example.demo.supporter.imageSearch.service.ImageSearchResultService;

@RestController
@RequestMapping("/supporter/image-search/results")
@RequiredArgsConstructor
public class ImageSearchResultController {
    private final ImageSearchResultService service;

    @GetMapping("/{id}")
    public ResponseEntity<ImageSearchResult> get(@PathVariable Long id) {
        return ResponseEntity.ok(service.get(id));
    }

    @GetMapping
    public ResponseEntity<List<ImageSearchResult>> list() {
        return ResponseEntity.ok(service.getAll());
    }

    @PostMapping
    public ResponseEntity<Long> create(@RequestBody ImageSearchResult r) {
        return ResponseEntity.ok(service.create(r));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Integer> update(@PathVariable Long id, @RequestBody ImageSearchResult r) {
        r.setId(id);
        return ResponseEntity.ok(service.update(r));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Integer> delete(@PathVariable Long id) {
        return ResponseEntity.ok(service.delete(id));
    }
}