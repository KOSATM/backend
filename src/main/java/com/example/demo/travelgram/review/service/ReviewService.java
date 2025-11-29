package com.example.demo.travelgram.review.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.common.s3.service.S3Service;
import com.example.demo.travelgram.review.dao.ReviewHashtagDao;
import com.example.demo.travelgram.review.dao.ReviewPhotoDao;
import com.example.demo.travelgram.review.dao.ReviewPostDao;
import com.example.demo.travelgram.review.dto.entity.ReviewHashtagGroup;
import com.example.demo.travelgram.review.dto.entity.ReviewPhoto;
import com.example.demo.travelgram.review.dto.entity.ReviewPhotoGroup;
import com.example.demo.travelgram.review.dto.entity.ReviewPost;
import com.example.demo.travelgram.review.dto.request.ReviewPhotoOrderUpdateRequest;
import com.example.demo.travelgram.review.dto.request.ReviewPhotoOrderUpdateRequest.PhotoOrderItem;
import com.example.demo.travelgram.review.dto.request.ReviewPhotoUploadRequest;
import com.example.demo.travelgram.review.dto.response.ReviewCreateResponse;
import com.example.demo.travelgram.review.dto.response.ReviewPhotoUploadResponse;

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

        // 결과 리턴
        return new ReviewCreateResponse(post.getId(), photoGroup.getId(),hashtagGroup.getId());
    }

    // ======================================
    // 2) 사진 업로드/순서 영역
    // ======================================

    public ReviewPhotoUploadResponse uploadPhoto(ReviewPhotoUploadRequest dto, MultipartFile file) {
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
        String s3Url = null;
        try {
            log.info("Attempting S3 upload via s3Service...");
            s3Url = s3Service.uploadFile(file, storedName); // S3 업로드
            log.info("S3 upload successful. URL: {}", s3Url);
        } catch (Exception e) {
            log.error("🛑 CRITICAL S3 UPLOAD FAILURE for file {}", storedName, e);
            // S3 업로드 실패 시 Custom Exception을 던지거나, RuntimeException으로 변환하여 상위로 전달
            throw new RuntimeException("S3 file upload failed", e);
        }

        // 5) DB에 저장할 엔티티 생성
        ReviewPhoto photo = ReviewPhoto.builder()
                .photoGroupId(dto.getPhotoGroupId())
                .orderIndex(dto.getOrderIndex())
                .fileUrl(s3Url)
                .build();

        // 6) DB 저장
        reviewPhotoDao.insertReviewPhoto(photo);

        return new ReviewPhotoUploadResponse(photo.getId(), photo.getFileUrl(), photo.getOrderIndex());

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

}
