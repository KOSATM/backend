package com.example.demo.supporter.imageSearch.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.example.demo.common.s3.service.S3Service;
import com.example.demo.supporter.imageSearch.dto.request.PlaceCandidateRequest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;

@Service
@Slf4j
@RequiredArgsConstructor
public class ImageProcessorService {
    private final S3Service s3Service;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    public record ImageUrlResult(String originalUrl, String thumbnailUrl) {
    }

    public ImageUrlResult processAndStoreImageFromUrl(PlaceCandidateRequest candidate) {
        
        // 1) 외부 URL에 접속해서 이미지 데이터를 메모리에 저장
        byte[] imageBytes = downloadImageBytes(candidate.getImageUrl());
        log.info("이미지 다운로드 성공, 크기 : {} bytes", imageBytes.length);

        // 2) 이미지를 JPEG 형식으로 변환
        byte[] originalJpegBytes = imageConverterToJpeg(imageBytes);
        byte[] ThumbnailJpegBytes = createThumbnail(imageBytes, 100);

        // 3) S3에 업로드, 변환된 파일URL 얻기
        String originalName = "images/original/" + UUID.randomUUID() + ".jpeg";
        String thumbnailName = "images/thumbnail/" + UUID.randomUUID() + ".jpeg";
        String originalUrl = s3Service.uploadFile2(originalJpegBytes, originalName);
        String thumbnailUrl = s3Service.uploadFile2(ThumbnailJpegBytes, thumbnailName);

        // 4) S3 image URL을 객체에 담아 반환
        ImageUrlResult result = new ImageUrlResult(originalUrl, thumbnailUrl);
        return result;
    }

    private byte[] downloadImageBytes(String imageUrl) {
        try {
            URL url = new URL(imageUrl);
            InputStream inputStream = url.openStream();
            return inputStream.readAllBytes();
        } catch (Exception e) {
            log.error("이미지 다운로드 중 오류 발생 : {}" + imageUrl, e);
            return null;
        }
    }

    private byte[] imageConverterToJpeg(byte[] imageBytes) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            Thumbnails.of(new ByteArrayInputStream(imageBytes))
                  .scale(1.0) //원본 사이즈 유지
                  .outputFormat("jpeg")
                  .toOutputStream(outputStream);
            byte[] jpegBytes = outputStream.toByteArray();
            log.info("이미지 JPEG 변환 성공, 크기 : {} bytes", jpegBytes.length);
            return jpegBytes;
        } catch (Exception e) {
            log.error("이미지 JPEG 변환 중 오류 발생 : {}", e.getMessage());
            return null;
        }
    }

    private byte[] createThumbnail(byte[] imageBytes, int width) {
        
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            Thumbnails.of(new ByteArrayInputStream(imageBytes))
                .width(width)
                .outputFormat("jpeg")
                .toOutputStream(outputStream);
            byte[] thumbnailBytes = outputStream.toByteArray();
            return thumbnailBytes;
        } catch (Exception e) {
            log.error("썸네일 생성 중 오류 발생 : {}", e.getMessage());
            return null;
        }
    }
}
