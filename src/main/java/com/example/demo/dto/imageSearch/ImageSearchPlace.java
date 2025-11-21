package com.example.demo.dto.imageSearch;

import java.time.OffsetDateTime;
import lombok.Data;

@Data
public class ImageSearchPlace {
    private Long id;
    private Long userId;
    private OffsetDateTime createdAt;
    private String actionType;
}
