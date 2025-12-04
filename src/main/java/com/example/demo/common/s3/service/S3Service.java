package com.example.demo.common.s3.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;

import lombok.RequiredArgsConstructor;

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

            // 업로드 후 접근 URL 반환
            return String.format(
                "https://%s.s3.%s.amazonaws.com/%s",
                bucket,
                region,
                storedName
            );

        } catch (IOException e) {
            throw new RuntimeException("S3 upload failed", e);
        }
    }

    public String uploadFile(byte[] imageBytes, String storedName) {
        // 1) 메타 데이터 생성 (파일 크기, 종류 등)
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(imageBytes.length);
        metadata.setContentType("image/jpeg");

        InputStream inputStream = new ByteArrayInputStream(imageBytes); //byte[] -> InputStream
        PutObjectRequest putObjectRequest = new PutObjectRequest(bucket, storedName, inputStream, metadata); //업로드 요청 객체 생성
        amazonS3.putObject(putObjectRequest);

        return amazonS3.getUrl(bucket, storedName).toString(); //업로드된 파일의 URL 반환
    }
}
