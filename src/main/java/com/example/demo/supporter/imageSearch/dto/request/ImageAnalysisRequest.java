package com.example.demo.supporter.imageSearch.dto.request;

import org.springframework.web.multipart.MultipartFile;

import lombok.Data;

@Data
public class ImageAnalysisRequest {
    private String question;
    private MultipartFile image;
}
