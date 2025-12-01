package com.example.demo.supporter.imageSearch.dto.entity;

import lombok.Data;

@Data
public class ImageSearchCandidate {
    private Long id; //PK
    private Long imageSearchSessionId; //FK
    private Long imagePlaceId; //FK
    private Boolean isSelected;
    private Long rank;
}