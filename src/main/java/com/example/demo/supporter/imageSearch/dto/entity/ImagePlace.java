package com.example.demo.supporter.imageSearch.dto.entity;

import lombok.Data;

@Data
public class ImagePlace {
    private Long id;
    private String name;
    private String description;
    private double lat;
    private double lng;
    private String address;
    private String placeType;
    private String internalOriginalUrl;
    private String internalThumbnailUrl;
    private String externalImageUrl;
    private ImageStatusEnum imageStatus;
}
