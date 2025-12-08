package com.example.demo.travelgram.review.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.common.s3.service.S3Service;
import com.example.demo.travelgram.review.ai.agent.ReviewImageAnalysisAgent;
import com.example.demo.travelgram.review.dao.ReviewHashtagDao;
import com.example.demo.travelgram.review.dao.ReviewPhotoDao;
import com.example.demo.travelgram.review.dao.ReviewPostDao;
import com.example.demo.travelgram.review.dto.entity.ReviewHashtagGroup;
import com.example.demo.travelgram.review.dto.entity.ReviewPhoto;
import com.example.demo.travelgram.review.dto.entity.ReviewPhotoGroup;
import com.example.demo.travelgram.review.dto.entity.ReviewPost;
import com.example.demo.travelgram.review.dto.request.ReviewPhotoOrderUpdateRequest;
import com.example.demo.travelgram.review.dto.request.ReviewPhotoOrderUpdateRequest.PhotoOrderItem;
import com.example.demo.travelgram.review.dto.request.ReviewPhotosAnalysisRequest;
import com.example.demo.travelgram.review.dto.response.PhotoAnalysisResult;
import com.example.demo.travelgram.review.dto.response.ReviewCreateResponse;
import com.example.demo.travelgram.review.dto.response.ReviewPhotoUploadResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final S3Service s3Service;

    private final ReviewPhotoDao reviewPhotoDao;
    private final ReviewPostDao reviewPostDao;
    private final ReviewHashtagDao reviewHashtagDao;

    private final ReviewImageAnalysisAgent reviewImageAnalysisAgent;
    private final ReviewAnalysisService reviewAnalysisService;
    private final ObjectMapper objectMapper;

    // ======================================
    // 1) 리뷰 포스트 영역
    // ======================================

    @Transactional
    public ReviewCreateResponse createReview(Long planId) {
        ReviewPost post = ReviewPost.builder()
                .planId(planId)
                .build();

        // 2. DB insert → post.id 자동 채워짐
        reviewPostDao.insertDraft(post);

        // 3. photo_group, hashtag_group 생성 시 reviewPostId 사용
        ReviewPhotoGroup photoGroup = ReviewPhotoGroup.builder()
                .reviewPostId(post.getId())
                .build();
        ReviewHashtagGroup hashtagGroup = ReviewHashtagGroup.builder()
                .reviewPostId(post.getId())
                .build();

        // 4. DB insert -> group.id 자동 생성됨
        reviewPhotoDao.insertReviewPhotoGroup(photoGroup);
        reviewHashtagDao.insertHashtagGroup(hashtagGroup);
        // 5. 리뷰 포스트에 그룹 아이디 업데이트
        reviewPostDao.updateReviewPostGroupId(post.getId(), photoGroup.getId(), hashtagGroup.getId());

        // 결과 리턴
        return new ReviewCreateResponse(post.getId(), photoGroup.getId(), hashtagGroup.getId());
    }

    // ======================================
    // 2) 사진 업로드 (JSON 파싱 로직 완전 삭제 버전)
    // ======================================
    public List<ReviewPhotoUploadResponse> uploadPhotosBatch(
            List<MultipartFile> files,
            Long photoGroupId, // 👈 JSON 대신 그냥 받음
            Integer startOrderIndex // 👈 JSON 대신 그냥 받음
    ) {

        // 1. 결과 담을 리스트
        List<ReviewPhotoUploadResponse> results = new ArrayList<>();

        // 2. 파일 리스트를 돌면서 순서대로 처리
        for (int i = 0; i < files.size(); i++) {
            MultipartFile file = files.get(i);

            // ⭐ 핵심 로직: 순서는 (시작번호 + 현재 인덱스)로 자동 계산
            int currentOrder = startOrderIndex + i;
            // 3. 내부 메서드로 처리 위임
            ReviewPhotoUploadResponse response = processSinglePhotoUpload(file, photoGroupId, currentOrder);
            results.add(response);
        }

        return results;
    }

    // 내부 처리 메서드 (파라미터가 DTO에서 단순 변수들로 바뀜)
    private ReviewPhotoUploadResponse processSinglePhotoUpload(
            MultipartFile file,
            Long photoGroupId,
            int orderIndex) {
        // 1) 파일 비어있으면 예외 처리
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("file is empty");
        }
        // 2) 확장자 추출
        String originalName = file.getOriginalFilename();

        String folder = "reviewPhotos/";
        // 3) UUID 파일명 생성
        if (originalName == null || !originalName.contains(".")) {
            originalName = "unknown_" + UUID.randomUUID();
        }
        String ext = "";
        int idx = originalName.lastIndexOf(".");
        if (idx > -1) {
            ext = originalName.substring(idx);
        }
        String storedName = folder + UUID.randomUUID().toString() + ext;
        // 4) S3 업로드
        String s3Url;
        try {
            s3Url = s3Service.uploadFile(file, storedName);
        } catch (Exception e) {
            throw new RuntimeException("S3 upload failed", e);
        }

        // 2. DB 저장 (AI 요약(summary)은 일단 null 또는 "분석 중..."으로 저장)
        ReviewPhoto photo = ReviewPhoto.builder()
                .photoGroupId(photoGroupId)
                .orderIndex(orderIndex)
                .fileUrl(s3Url)
                .summary(null) // 나중에 채워짐
                .build();

        reviewPhotoDao.insertReviewPhoto(photo);

        // 3. ★ 비동기 AI 분석 요청 (기다리지 않고 바로 넘어감)
        try {
            reviewAnalysisService.analyzePhotoAndUpdateDb(
                    photo.getId(),
                    file.getContentType(),
                    file.getBytes() // IO 발생하므로 주의, 파일이 너무 크면 InputStream 방식 고려
            );
        } catch (IOException e) {
            log.error("이미지 바이트 읽기 실패", e);
        }

        return new ReviewPhotoUploadResponse(photo.getId(), photo.getFileUrl(), photo.getOrderIndex());

    }


    public List<ReviewPhoto> getReviewPhotos(Long photoGroupId) {
        return reviewPhotoDao.selectReviewPhotosByPhotoGroupId(photoGroupId);
    }

    @Transactional
    public void updatePhotoOrder(ReviewPhotoOrderUpdateRequest request) {
        for (PhotoOrderItem item : request.getPhotos()) {
            reviewPhotoDao.updatePhotoOrder(
                    item.getPhotoId(),
                    item.getOrderIndex(),
                    request.getPhotoGroupId());
        }
    }

    public void analyzeTripContext(Long photoGroupId) {

        // 1. [DB 조회] 해당 그룹의 모든 사진 요약 가져오기 (리스트로 반환됨)
        // for문 필요 없음! MyBatis가 List로 줍니다.
        List<String> summaryList = reviewPhotoDao.selectPhotoSummariesByPhotoGroupId(photoGroupId);

        // 요약된 사진이 하나도 없으면 분석 중단
        if (summaryList.isEmpty())
            return;

        // 2. [DTO 포장] LLM에게 보낼 요청 객체 생성
        ReviewPhotosAnalysisRequest requestDto = new ReviewPhotosAnalysisRequest();
        requestDto.setPhotoGroupId(photoGroupId);
        requestDto.setSummaries(summaryList); // DB에서 가져온 리스트를 바로 넣음

        // 3. [LLM 호출] 에이전트에게 DTO를 넘김
        // 에이전트 코드는 아래 3단계에서 설명
        PhotoAnalysisResult result = reviewImageAnalysisAgent.analyzeTripContext(requestDto.getSummaries());

        reviewPostDao.updateReviewPostMood(photoGroupId, result.getOverallMood(), result.getTravelType());

        log.info("📊 여행 분석 완료: Type={}, Mood={}", result.getTravelType(), result.getOverallMood());

    }

    @Transactional
    public void selectStyle(Long reviewPostId, Long reviewStyleId) {
        log.info("리뷰 스타일 선택 업데이트 - reviewPostId: {}, reviewStyleId: {}", reviewPostId, reviewStyleId);
        
        // DAO 호출하여 업데이트 수행
        reviewPostDao.updateReviewPostStyleIdById(reviewPostId, reviewStyleId);
    }

    @Transactional
    public void updateCaption(Long reviewPostId, String caption){
        log.info("리뷰 캡션 업데이트 - reviewPostId: {}, caption: {}", reviewPostId, caption);

        reviewPostDao.updateReviewPostCaptionIdById(reviewPostId, caption);
    }

    @Transactional
    public void updateHashtags(Long hashtagGroupId, List<String> names){

        // 1. 기존 태그들 삭제 (초기화)
        reviewHashtagDao.deleteHashtagsByHashtagGroupId(hashtagGroupId);

        // 2. 선택된 태그가 있을 때만 insert 수행
        // (사용자가 태그를 다 지워서 빈 리스트가 올 수도 있으므로 체크)
        if (names != null && !names.isEmpty()) {
            reviewHashtagDao.insertHashtagList(hashtagGroupId, names);
        }
    }
}