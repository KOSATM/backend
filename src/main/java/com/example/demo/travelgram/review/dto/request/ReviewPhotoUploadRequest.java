package com.example.demo.travelgram.review.dto.request;

import lombok.Data;

@Data
public class ReviewPhotoUploadRequest {
    private Long groupId;
    private Integer orderIndex;
}
