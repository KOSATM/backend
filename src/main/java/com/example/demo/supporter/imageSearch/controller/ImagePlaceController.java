package com.example.demo.supporter.imageSearch.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.supporter.imageSearch.service.ImagePlaceService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/supporter/image-search/places")
@RequiredArgsConstructor
public class ImagePlaceController {
    private final ImagePlaceService service;

    // @GetMapping("/{id}")
    // public ResponseEntity<ImagePlace> get(@PathVariable Long id) {
    //     return ResponseEntity.ok(service.get(id));
    // }

    // @GetMapping
    // public ResponseEntity<List<ImagePlace>> list() {
    //     return ResponseEntity.ok(service.getAll());
    // }

    // @PostMapping
    // public ResponseEntity<Long> create(@RequestBody ImagePlace p) {
    //     return ResponseEntity.ok(service.create(p));
    // }

    // @PutMapping("/{id}")
    // public ResponseEntity<Integer> update(@PathVariable Long id, @RequestBody ImagePlace p) {
    //     p.setId(id);
    //     return ResponseEntity.ok(service.update(p));
    // }

    // @DeleteMapping("/{id}")
    // public ResponseEntity<Integer> delete(@PathVariable Long id) {
    //     return ResponseEntity.ok(service.delete(id));
    // }
}