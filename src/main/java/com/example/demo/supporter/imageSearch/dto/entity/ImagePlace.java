package com.example.demo.supporter.imageSearch.dto.entity;

import lombok.Data;

@Data
public class ImagePlace {
    private Long id; //PK
    private String name;
    private String description;
    private double lat; //위도
    private double lng; //경도
    private String address;
    private String placeType; //"travel", "restaurant", "accommodation"
    private String internalOriginalUrl;
    private String internalThumbnailUrl;
    private String externalImageUrl;
    private ImageStatusEnum imageStatus;
}
    