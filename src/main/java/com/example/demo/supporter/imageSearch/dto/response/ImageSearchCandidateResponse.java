package com.example.demo.supporter.imageSearch.dto.response;

import lombok.Data;

@Data
public class ImageSearchCandidateResponse {
    private Long id;
    private Long imageSearchSessionId;
    private Long imagePlaceId;
    private Boolean isSelected;
    private Long rank;
}
