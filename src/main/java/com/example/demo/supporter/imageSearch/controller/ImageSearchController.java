package com.example.demo.supporter.imageSearch.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.supporter.imageSearch.dto.entity.ImageSearchCandidate;
import com.example.demo.supporter.imageSearch.dto.request.PlaceCandidateRequest;
import com.example.demo.supporter.imageSearch.dto.response.PlaceCandidateResponse;
import com.example.demo.supporter.imageSearch.dto.response.SessionWithCandidatesResponse;
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
    public ResponseEntity<Long> savePlaceCandidates(
        @RequestParam("userId") Long userId,
        @RequestBody List<PlaceCandidateRequest> candidates) {

        Long sessionId = service.savePlaceCandidates(userId, candidates);
        return ResponseEntity.ok(sessionId);
    }

    @PutMapping("/{candidateId}/action-type")
    public ResponseEntity<Integer> alterActionType(
        @PathVariable("candidateId") Long candidateId,
        @RequestParam("actionType") String actionType) {
        return ResponseEntity.ok(service.updateSessionActionType(candidateId, actionType));
    }

    @DeleteMapping("/{candidateId}")
    public ResponseEntity<Integer> delete(@PathVariable("candidateId") Long candidateId) {
        return ResponseEntity.ok(service.delete(candidateId));
    }

    @GetMapping("/sessions")
    public ResponseEntity<List<SessionWithCandidatesResponse>> getSessionsByUserId(
        @RequestParam("userId") Long userId) {
        return ResponseEntity.ok(service.getSessionsByUserId(userId));
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
}