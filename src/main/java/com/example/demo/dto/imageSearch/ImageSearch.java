package com.example.demo.dto.imageSearch;

import java.time.OffsetDateTime;
import lombok.Data;

@Data
public class ImageSearch {
    private Long id;
    private Long userId;
    private OffsetDateTime createdAt;
    private String actionType;
}
