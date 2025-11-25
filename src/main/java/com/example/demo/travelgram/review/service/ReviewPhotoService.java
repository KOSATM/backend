package com.example.demo.travelgram.review.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.common.s3.service.S3Service;
import com.example.demo.travelgram.review.dao.ReviewPhotoDao;
import com.example.demo.travelgram.review.dto.entity.ReviewPhoto;
import com.example.demo.travelgram.review.dto.request.ReviewPhotoUploadRequest;
import com.example.demo.travelgram.review.dto.response.ReviewPhotoUploadResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReviewPhotoService {

    private final S3Service s3Service;
    private final ReviewPhotoDao reviewPhotoDao;

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
        String s3Url = s3Service.uploadFile(file, storedName); // S3 업로드

        // 5) DB에 저장할 엔티티 생성
        // 📌 세터 없이 빌더로 엔티티 생성
        ReviewPhoto photo = ReviewPhoto.builder()
                .groupId(dto.getGroupId())
                .orderIndex(dto.getOrderIndex())
                .originalName(originalName)
                .storedName(storedName)
                .fileUrl(s3Url)
                .build();

        // 6) DB 저장
        reviewPhotoDao.insert(photo);

        return new ReviewPhotoUploadResponse(photo.getId(),photo.getFileUrl(),photo.getOrderIndex());

    }
}
