package com.example.demo.travelgram.review.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.common.s3.service.S3Service;
import com.example.demo.travelgram.review.dao.ReviewPhotoDao;
import com.example.demo.travelgram.review.dto.entity.ReviewPhoto;
import com.example.demo.travelgram.review.dto.request.PhotoUploadRequest;
import com.example.demo.travelgram.review.dto.response.PhotoUploadResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReviewPhotoService {

    private final S3Service s3Service;
    private final ReviewPhotoDao reviewPhotoMapper;

    public PhotoUploadResponse uploadPhoto(PhotoUploadRequest req, MultipartFile file) {

        // 1) UUID 저장명 생성
        String storedName = UUID.randomUUID().toString() + "_" + req.getFileName();

        // 2) S3 업로드 → 실제 URL 반환
        String fileUrl = s3Service.uploadFileToS3(file, storedName);

        // 3) DB 저장할 엔티티 구성
        ReviewPhoto photo = ReviewPhoto.builder()
                .fileUrl(fileUrl)
                .orderIndex(req.getOrderIndex()) // ❗ 업로드 순서 그대로 삽입
                .groupId(req.getGroupId())
                .build();

        reviewPhotoMapper.insert(photo);

        // 4) insert 실행
        reviewPhotoMapper.insert(photo);

        // 5) 응답
        return new PhotoUploadResponse(photo.getId(), fileUrl);
    }
}
