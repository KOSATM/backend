package com.example.demo.dto.image;

import lombok.Data;

@Data
public class ImagePlaceSearchResultDto {
    private Long id;
    private Long imageSearchHistoryId;
    private Long placeId;
    private Boolean isSelected;
    private Long rank;
}
