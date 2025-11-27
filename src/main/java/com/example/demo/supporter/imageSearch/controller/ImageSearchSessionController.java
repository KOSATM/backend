package com.example.demo.supporter.imageSearch.controller;

import com.example.demo.supporter.imageSearch.dto.entity.ImageSearchSession;
import com.example.demo.supporter.imageSearch.service.ImageSearchSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/supporter/image-search/places")
@RequiredArgsConstructor
public class ImageSearchSessionController {
    private final ImageSearchSessionService service;

    @GetMapping("/{id}")
    public ResponseEntity<ImageSearchSession> get(@PathVariable Long id) {
        return ResponseEntity.ok(service.get(id));
    }

    @GetMapping
    public ResponseEntity<List<ImageSearchSession>> list() {
        return ResponseEntity.ok(service.getAll());
    }

    @PostMapping
    public ResponseEntity<Long> create(@RequestBody ImageSearchSession p) {
        return ResponseEntity.ok(service.create(p));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Integer> update(@PathVariable Long id, @RequestBody ImageSearchSession p) {
        p.setId(id);
        return ResponseEntity.ok(service.update(p));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Integer> delete(@PathVariable Long id) {
        return ResponseEntity.ok(service.delete(id));
    }
}