package com.example.demo.supporter.imageSearch.dto.entity;

import lombok.Data;

@Data
public class ImageSearchCandidate {
    private Long id;
    private Long imageSearchPlaceId;
    private Long placeId;
    private Boolean isSelected;
    private Long rank;
}