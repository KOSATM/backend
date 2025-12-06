package com.example.demo.travelgram.review.controller;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.common.global.annotation.NoWrap;
import com.example.demo.travelgram.review.dto.entity.ReviewPhoto;
import com.example.demo.travelgram.review.dto.request.ReviewCreateRequest;
import com.example.demo.travelgram.review.dto.request.ReviewPhotoOrderUpdateRequest;
import com.example.demo.travelgram.review.dto.response.PhotoAnalysisResult;
import com.example.demo.travelgram.review.dto.response.ReviewCreateResponse;
import com.example.demo.travelgram.review.dto.response.ReviewPhotoUploadResponse;
import com.example.demo.travelgram.review.service.ReviewService;
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

    @GetMapping("/photos")
    public ResponseEntity<List<ReviewPhoto>> getPhotosByGroup(@RequestParam("photoGroupId") Long photoGroupId) {
        // Service에 해당 메서드가 없다면 추가해야 합니다 (아래 설명)
        List<ReviewPhoto> photos = reviewService.getReviewPhotos(photoGroupId);
        return ResponseEntity.ok(photos);
    }
    @PutMapping("/photo/order")
    public ResponseEntity<?> updatePhotoOrder(@RequestBody ReviewPhotoOrderUpdateRequest request) {
        reviewService.updatePhotoOrder(request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/photo/analyze")
    public ResponseEntity<Void> updatePhotoMoods(@RequestParam("photoGroupId") Long photoGroupId) {

        // 서비스 실행 (내부에서 DB 업데이트까지 완료됨)
        reviewService.analyzeTripContext(photoGroupId);

        // 내용물 없이 성공 신호(200 OK)만 보냄
        return ResponseEntity.ok().build();
    }

    @PostMapping("/style/select")
    public ResponseEntity<Void> selectStyle(@RequestParam("reviewPostId") Long reviewPostId, @RequestParam("reviewStyleId") Long reviewStyleId) {
        reviewService.selectStyle(reviewPostId,reviewStyleId);
        return ResponseEntity.ok().build();
    }
}
