package com.example.demo.travelgram.review.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.travelgram.review.dao.ReviewPhotoDao;
import com.example.demo.travelgram.review.dto.entity.ReviewPhoto;
import com.example.demo.travelgram.review.dto.request.PhotoUploadRequest;
import com.example.demo.travelgram.review.dto.response.PhotoUploadResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReviewPhotoService {

    private final S3Service s3Service;
    private final ReviewPhotoDao reviewPhotoDao;

    public PhotoUploadResponse uploadPhoto(PhotoUploadRequest req, MultipartFile file) {

        // 1) UUID 저장명 생성
        String storedName = UUID.randomUUID().toString() + "_" + req.getFileName();

        // 2) S3 업로드 → 실제 URL 반환
        String fileUrl = s3Service.uploadFileToS3(file, storedName);

        // 3) DB 저장할 엔티티 구성
        ReviewPhoto photo = new ReviewPhoto();
        photo.setFileUrl(fileUrl);
        photo.setGroupId(req.getGroupId());
        photo.setOrderIndex(0);  // 최초 업로드 시 0, 이후 순서변경 API에서 수정
        photo.setLat(null);
        photo.setLng(null);
        photo.setTakenAt(null);

        // 4) insert 실행
        reviewPhotoDao.insert(photo);

        // 5) 응답
        return new PhotoUploadResponse(photo.getId(), fileUrl);
    }
}
