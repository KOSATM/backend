package com.example.demo.travelgram.review.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ReviewPhotoUploadResponse {
    private Long photoId;
    private String fileUrl;
}