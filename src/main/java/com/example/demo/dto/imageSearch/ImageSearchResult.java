package com.example.demo.dto.imageSearch;

import lombok.Data;

@Data
public class ImageSearchResult {
    private Long id;
    private Long imageSearchPlacesId;
    private Long placeId;
    private Boolean isSelected;
    private Long rank;
}
