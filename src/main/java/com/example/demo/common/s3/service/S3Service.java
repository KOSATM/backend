package com.example.demo.common.s3.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.PutObjectRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class S3Service {

    private final AmazonS3 amazonS3;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    public String uploadFileToS3(MultipartFile file, String storedName) {
        try {
            amazonS3.putObject(
                new PutObjectRequest(bucket, storedName, file.getInputStream(), null)
                    .withCannedAcl(CannedAccessControlList.PublicRead)
            );
        } catch (Exception e) {
            throw new RuntimeException("S3 업로드 실패", e);
        }

        return "https://" + bucket + ".s3.amazonaws.com/" + storedName;
    }
}
