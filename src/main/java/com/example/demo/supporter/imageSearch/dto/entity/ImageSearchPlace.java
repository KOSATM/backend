package com.example.demo.supporter.imageSearch.dto.entity;


import java.time.OffsetDateTime;
import lombok.Data;

@Data
public class ImageSearchPlace {
    private Long id;
    private Long userId;
    private OffsetDateTime createdAt;
    private String actionType;
}
