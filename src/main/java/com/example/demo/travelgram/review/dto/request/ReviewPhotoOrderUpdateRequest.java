package com.example.demo.travelgram.review.dto.request;

import java.util.List;

import lombok.Data;

@Data
public class ReviewPhotoOrderUpdateRequest {
    private Long photoGroupId;
    private List<PhotoOrderItem> photos;
    
    @Data
    public static class PhotoOrderItem {
        private Long photoId;
        private Integer orderIndex;
    }
}
