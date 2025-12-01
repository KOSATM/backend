package com.example.demo.travelgram.review.dto.request;

import lombok.Data;

@Data
public class ReviewPhotoUploadRequest {
    private Long photoGroupId;
    private String fileName;
    private Integer orderIndex;
}
