package com.example.demo.travelgram.review.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.travelgram.review.dto.entity.ReviewPost;
import com.example.demo.travelgram.review.dto.request.ReviewPhotoUploadRequest;
import com.example.demo.travelgram.review.dto.response.ReviewPhotoUploadResponse;
import com.example.demo.travelgram.review.service.ReviewPhotoService;
import com.example.demo.travelgram.review.service.ReviewPostService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/review")
public class ReviewController {
    private final ReviewPhotoService reviewPhotoService;
    private final ReviewPostService reviewPostService;
    private final ObjectMapper objectMapper; // ObjectMapper 주입

    @PostMapping(value = "/photos/upload", consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })

    public ResponseEntity<?> uploadReviewPhoto(
            @RequestPart("dataListJson") String dataListJsonString,
            @RequestPart("files") List<MultipartFile> files) throws Exception {

        // 1. JSON String을 Map 리스트로 수동 파싱
        List<Map<String, Object>> dataListJson;
        try {
            dataListJson = objectMapper.readValue(
                dataListJsonString, 
                new TypeReference<List<Map<String, Object>>>() {}
            );
        } catch (Exception e) {
            log.error("Failed to parse dataListJson string: {}", dataListJsonString, e);
            return ResponseEntity.badRequest().body("Invalid JSON format for dataListJson");
        }


    // 2. Map → DTO 변환
    List<ReviewPhotoUploadRequest> dtoList = new ArrayList<>();

    for (Map<String, Object> map : dataListJson) {
        ReviewPhotoUploadRequest dto = new ReviewPhotoUploadRequest();

        // 주의: JSON에서 숫자는 기본적으로 Integer 또는 Long으로 오므로 Number로 처리합니다.
        dto.setGroupId(((Number) map.get("groupId")).longValue());
        dto.setFileName((String) map.get("fileName"));
        dto.setOrderIndex(((Number) map.get("orderIndex")).intValue());

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
            ReviewPhotoUploadResponse res = reviewPhotoService.uploadPhoto(dto, file);
            result.add(res);
        }

        return ResponseEntity.ok(result);
    }

    @PostMapping("/photos/group")
    public ResponseEntity<?> createPhotoGroup() {
        Long groupId = reviewPhotoService.createPhotoGroup();
        return ResponseEntity.ok(Map.of("groupId", groupId));
    }

    @PostMapping("/post")
    public Long createReviewPost(@RequestBody ReviewPost post) {
        return reviewPostService.createReviewPost(post);
    }

    @GetMapping("/posts/{planId}")
    public List<ReviewPost> getReviewPosts(@PathVariable Long planId) {
        return reviewPostService.getReviewPostsByPlan(planId);
    }
}
