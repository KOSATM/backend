package com.example.demo.supporter.imageSearch.dto.response;

import lombok.Data;

@Data
public class ImagePlaceResponse {
    private Long id; //PK

    private String name;
    private String address;
    private double lat; // 위도
    private double lng; // 경도
    private String placeType; // "travel", "restaurant", "accommodation"
    private String description;
    private String internalOriginalUrl;
    private String internalThumbnailUrl;
    private String externalImageUrl;
    private ImageStatusEnum imageStatus;

    public enum ImageStatusEnum {
        PENDING, READY, FAILED
    }
}
    