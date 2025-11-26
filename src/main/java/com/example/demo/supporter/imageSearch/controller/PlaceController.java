package com.example.demo.supporter.imageSearch.controller;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.example.demo.supporter.imageSearch.dto.entity.Place;
import com.example.demo.supporter.imageSearch.service.PlaceService;

@RestController
@RequestMapping("/supporter/places")
@RequiredArgsConstructor
public class PlaceController {
    private final PlaceService service;

    @GetMapping("/{id}")
    public ResponseEntity<Place> get(@PathVariable Long id) {
        return ResponseEntity.ok(service.get(id));
    }

    @GetMapping
    public ResponseEntity<List<Place>> list() {
        return ResponseEntity.ok(service.getAll());
    }

    @PostMapping
    public ResponseEntity<Long> create(@RequestBody Place p) {
        return ResponseEntity.ok(service.create(p));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Integer> update(@PathVariable Long id, @RequestBody Place p) {
        p.setId(id);
        return ResponseEntity.ok(service.update(p));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Integer> delete(@PathVariable Long id) {
        return ResponseEntity.ok(service.delete(id));
    }
}