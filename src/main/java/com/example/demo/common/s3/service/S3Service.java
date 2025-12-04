package com.example.demo.common.s3.service;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3Service {

    private final AmazonS3 amazonS3;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    @Value("${cloud.aws.region.static}")
    private String region;

    public String uploadFile(MultipartFile file, String storedName) {

        try {
            // 메타데이터
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.getSize());
            metadata.setContentType(file.getContentType());

            // S3 업로드
            amazonS3.putObject(bucket, storedName, file.getInputStream(), metadata);

            // ✅ [추천] URL 생성을 SDK에게 맡기기 (더 안전함)
            return amazonS3.getUrl(bucket, storedName).toString();

        } catch (IOException e) {
            throw new RuntimeException("S3 upload failed", e);
        }
    }

    public void deleteFile(String fileUrl) {
    try {
        // fileUrl에서 key(storedName)만 추출하는 로직 필요
        // 예: https://bucket.s3.region.amazonaws.com/reviewPhotos/abc.jpg 
        // -> reviewPhotos/abc.jpg 추출
        String splitStr = ".com/";
        String fileName = fileUrl.substring(fileUrl.lastIndexOf(splitStr) + splitStr.length());

        amazonS3.deleteObject(bucket, fileName);
    } catch (Exception e) {
        // 삭제 실패는 로그만 남기고 넘어가는 경우가 많음 (시스템 장애로 번지지 않게)
        log.error("Error deleting file from S3: " + fileUrl, e);
    }
}
}
