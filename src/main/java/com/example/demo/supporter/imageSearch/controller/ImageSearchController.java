package com.example.demo.supporter.imageSearch.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.supporter.imageSearch.dto.request.PlaceCandidateRequest;
import com.example.demo.supporter.imageSearch.dto.response.PlaceCandidateResponse;
import com.example.demo.supporter.imageSearch.service.ImageSearchService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/supporter/image-search/candidates")
@RequiredArgsConstructor
public class ImageSearchController {

    private final ImageSearchService service;

    //이미지 agent 생성 및 후보자 생성
    @PostMapping
    public ResponseEntity<List<PlaceCandidateResponse>> recommendPlacesByImage(
            @RequestParam("placeType") String placeType,
            @RequestParam("image") MultipartFile image,
            @RequestParam("address") String address) throws Exception {
        List<PlaceCandidateResponse> candidates = service.processImageForPlaceRecommendation(placeType, image, address);
        return ResponseEntity.ok(candidates);
    }

    //후보자 저장
    @PostMapping("/save")
    public ResponseEntity<Void> savePlaceCandidates(@RequestBody List<PlaceCandidateRequest> candidates) {
        service.savePlaceCandidates(candidates);
        return ResponseEntity.ok().build();
    }

    // @GetMapping("/{id}")
    // public ResponseEntity<ImageSearchCandidate> get(@PathVariable Long id) {
    // return ResponseEntity.ok(service.get(id));
    // }

    // @GetMapping
    // public ResponseEntity<List<ImageSearchCandidate>> list() {
    // return ResponseEntity.ok(service.getAll());
    // }

    // @PostMapping
    // public ResponseEntity<Long> create(@RequestBody ImageSearchCandidate r) {
    // return ResponseEntity.ok(service.create(r));
    // }

    // @PutMapping("/{id}")
    // public ResponseEntity<Integer> update(@PathVariable Long id, @RequestBody
    // ImageSearchCandidate r) {
    // r.setId(id);
    // return ResponseEntity.ok(service.update(r));
    // }

    // @DeleteMapping("/{id}")
    // public ResponseEntity<Integer> delete(@PathVariable Long id) {
    // return ResponseEntity.ok(service.delete(id));
    // }
}