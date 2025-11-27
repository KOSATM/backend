package com.example.demo.travelgram.review.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.travelgram.aiReview.dto.request.ReviewContentUpdateRequest;
import com.example.demo.travelgram.aiReview.dto.request.ReviewStyleSelectRequest;
import com.example.demo.travelgram.aiReview.dto.response.AiReviewStyleResponse;
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
@RequestMapping("/review")
public class ReviewController {
    private final ReviewService reviewService;
    private final ObjectMapper objectMapper; // ObjectMapper 주입


    // ======================================
    // 1) 리뷰 포스트 영역
    // ======================================

    @PostMapping("/create")
    public ResponseEntity<ReviewCreateResponse> createReview(
            @RequestBody ReviewCreateRequest request) {

        ReviewCreateResponse result = reviewService.createReview(request.getTravelPlanId());

        return ResponseEntity.ok(result);
    }

    // ======================================
    // 2) 사진 업로드/순서 영역
    // ======================================
    @PostMapping(value = "/photos/upload", consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
    public ResponseEntity<?> uploadReviewPhoto(
            @RequestPart("dataListJson") String dataListJsonString,
            @RequestPart("files") List<MultipartFile> files) throws Exception {

        // 1. JSON String을 Map 리스트로 수동 파싱
        List<Map<String, Object>> dataListJson;
        try {
            dataListJson = objectMapper.readValue(
                    dataListJsonString,
                    new TypeReference<List<Map<String, Object>>>() {
                    });
        } catch (Exception e) {
            log.error("Failed to parse dataListJson string: {}", dataListJsonString, e);
            return ResponseEntity.badRequest().body("Invalid JSON format for dataListJson");
        }

        // 2. Map → DTO 변환
        List<ReviewPhotoUploadRequest> dtoList = new ArrayList<>();

        for (Map<String, Object> map : dataListJson) {
            ReviewPhotoUploadRequest dto = new ReviewPhotoUploadRequest();

            // Use Optional to check for null and throw a descriptive exception if missing
            Number groupIdNumber = Optional.ofNullable(map.get("groupId"))
                    .map(obj -> (Number) obj)
                    .orElseThrow(() -> new IllegalArgumentException("Missing required parameter: 'groupId'"));

            Number orderIndexNumber = Optional.ofNullable(map.get("orderIndex"))
                    .map(obj -> (Number) obj)
                    .orElseThrow(() -> new IllegalArgumentException("Missing required parameter: 'orderIndex'"));

            dto.setGroupId(groupIdNumber.longValue());
            dto.setFileName((String) map.get("fileName"));
            dto.setOrderIndex(orderIndexNumber.intValue());

            dtoList.add(dto);
        }

        // 2) 파일 개수와 DTO 개수 체크
        if (dtoList.size() != files.size()) {
            throw new IllegalArgumentException("metadata count != file count");
        }

        // 3) 개별 업로드 처리
        List<Object> result = new ArrayList<>();
        for (int i = 0; i < files.size(); i++) {
            MultipartFile file = files.get(i);
            ReviewPhotoUploadRequest dto = dtoList.get(i);

            // 서비스 호출
            ReviewPhotoUploadResponse res = reviewService.uploadPhoto(dto, file);
            result.add(res);
        }

        return ResponseEntity.ok(result);
    }

    @PutMapping("/photo/order")
    public ResponseEntity<?> updatePhotoOrder(@RequestBody ReviewPhotoOrderUpdateRequest request) {
        reviewService.updatePhotoOrder(request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/photo/delete/{id}")
    public ResponseEntity<?> deletePhoto(@PathVariable ("id") Long id){
        reviewService.deletePhoto(id);
        return ResponseEntity.noContent().build();
    }


    // ======================================
    // 3) review_posts update, style 부터
    // ======================================

    // 2) 스타일 선택 적용 (AI 캡션 자동 반영)
    @PutMapping("/{postId}/style")
    public ResponseEntity<ReviewPostResponse> applyStyle(
            @PathVariable Long postId,
            @RequestBody ReviewStyleSelectRequest request
    ) {
        return ResponseEntity.ok(reviewService.applyStyle(postId, request));
    }

    // 3) 해시태그 선택 저장
    @PutMapping("/{postId}/hashtags")
    public ResponseEntity<Void> updateHashtags(
            @PathVariable Long postId,
            @RequestBody ReviewHashtagUpdateRequest request
    ) {
        reviewService.updateHashtags(postId, request);
        return ResponseEntity.ok().build();
    }

    // 4) 캡션 수정
    @PutMapping("/{postId}/content")
    public ResponseEntity<ReviewPostResponse> updateContent(
            @PathVariable Long postId,
            @RequestBody ReviewContentUpdateRequest request
    ) {
        return ResponseEntity.ok(reviewService.updateContent(postId, request));
    }

    // 5) 프리뷰 조회
    @GetMapping("/{postId}")
    public ResponseEntity<ReviewPreviewResponse> getPreview(
            @PathVariable Long postId
    ) {
        return ResponseEntity.ok(reviewService.getPreview(postId));
    }

    // 6) 게시하기
    @PutMapping("/{postId}/publish")
    public ResponseEntity<ReviewPostResponse> publish(
            @PathVariable Long postId
    ) {
        return ResponseEntity.ok(reviewService.publish(postId));
    }
    
}
