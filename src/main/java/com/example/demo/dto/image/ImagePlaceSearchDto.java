package com.example.demo.dto.image;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class ImagePlaceSearchDto {
    private Long id;
    private Long userId;
    private LocalDateTime searchedAt;
    private String actionType;
}
