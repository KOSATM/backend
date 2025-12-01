package com.example.demo.supporter.imageSearch.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.supporter.imageSearch.service.ImageSearchService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/supporter/image-search/candidates")
@RequiredArgsConstructor
public class ImageSearchController {

    private final ImageSearchService service;

    @PostMapping
    public void recommendPlacesByImage(
            @RequestParam("placeType") String placeType,
            @RequestParam("image") MultipartFile image) throws Exception {
        service.processImageForPlaceRecommendation(placeType, image);
    }
    
    // @GetMapping("/{id}")
    // public ResponseEntity<ImageSearchCandidate> get(@PathVariable Long id) {
    //     return ResponseEntity.ok(service.get(id));
    // }

    // @GetMapping
    // public ResponseEntity<List<ImageSearchCandidate>> list() {
    //     return ResponseEntity.ok(service.getAll());
    // }

    // @PostMapping
    // public ResponseEntity<Long> create(@RequestBody ImageSearchCandidate r) {
    //     return ResponseEntity.ok(service.create(r));
    // }

    // @PutMapping("/{id}")
    // public ResponseEntity<Integer> update(@PathVariable Long id, @RequestBody ImageSearchCandidate r) {
    //     r.setId(id);
    //     return ResponseEntity.ok(service.update(r));
    // }

    // @DeleteMapping("/{id}")
    // public ResponseEntity<Integer> delete(@PathVariable Long id) {
    //     return ResponseEntity.ok(service.delete(id));
    // }
}