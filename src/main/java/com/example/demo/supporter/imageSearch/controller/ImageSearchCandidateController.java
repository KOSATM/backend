package com.example.demo.supporter.imageSearch.controller;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.example.demo.supporter.imageSearch.dto.entity.ImageSearchCandidate;
import com.example.demo.supporter.imageSearch.service.ImageSearchCandidateService;

@RestController
@RequestMapping("/supporter/image-search/results")
@RequiredArgsConstructor
public class ImageSearchCandidateController {
    private final ImageSearchCandidateService service;

    @GetMapping("/{id}")
    public ResponseEntity<ImageSearchCandidate> get(@PathVariable Long id) {
        return ResponseEntity.ok(service.get(id));
    }

    @GetMapping
    public ResponseEntity<List<ImageSearchCandidate>> list() {
        return ResponseEntity.ok(service.getAll());
    }

    @PostMapping
    public ResponseEntity<Long> create(@RequestBody ImageSearchCandidate r) {
        return ResponseEntity.ok(service.create(r));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Integer> update(@PathVariable Long id, @RequestBody ImageSearchCandidate r) {
        r.setId(id);
        return ResponseEntity.ok(service.update(r));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Integer> delete(@PathVariable Long id) {
        return ResponseEntity.ok(service.delete(id));
    }
}