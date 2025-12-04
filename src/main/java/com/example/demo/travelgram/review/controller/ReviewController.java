package com.example.demo.travelgram.review.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.common.global.annotation.NoWrap;
import com.example.demo.travelgram.review.dto.request.*;
import com.example.demo.travelgram.review.dto.response.*;
import com.example.demo.travelgram.review.service.ReviewService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/reviews")
public class ReviewController {
    private final ReviewService reviewService;
    private final ObjectMapper objectMapper; // ObjectMapper 주입

    // ======================================
    // 1) 리뷰 포스트 영역
    // ======================================

    @PostMapping("/create")
    public ResponseEntity<ReviewCreateResponse> createReview(
            @RequestBody ReviewCreateRequest request) {

        ReviewCreateResponse result = reviewService.createReview(request.getPlanId());

        return ResponseEntity.ok(result);
    }

    // ======================================
    // 2) 사진 업로드/순서 영역
    // ======================================
    @NoWrap
    // Controller
    @PostMapping(value = "/photos/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadReviewPhotos(
            // ❌ 삭제: @RequestPart("dataListJson") String dataListJsonString

            // ✅ 추가: 프론트에서 보낸 photoGroupId (단순 텍스트는 @RequestParam 사용)
            @RequestParam("photoGroupId") Long photoGroupId,

            // ✅ 추가: 프론트에서 보낸 startOrderIndex
            @RequestParam("startOrderIndex") Integer startOrderIndex,

            // 그대로 유지: 파일 리스트
            @RequestPart("files") List<MultipartFile> files) {
        // JSON 파싱 로직 싹 다 삭제하고, 바로 서비스 호출!
        // (서비스 메서드 시그니처도 아까 우리가 수정했으므로 딱 맞습니다)
        List<ReviewPhotoUploadResponse> result = reviewService.uploadPhotosBatch(files, photoGroupId, startOrderIndex);

        return ResponseEntity.ok(result);
    }

    @PutMapping("/photo/order")
    public ResponseEntity<?> updatePhotoOrder(@RequestBody ReviewPhotoOrderUpdateRequest request) {
        reviewService.updatePhotoOrder(request);
        return ResponseEntity.noContent().build();
    }

}
