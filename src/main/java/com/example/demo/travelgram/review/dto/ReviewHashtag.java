package com.example.demo.travelgram.review.dto;

import java.time.OffsetDateTime;
import lombok.Data;

@Data
public class ReviewHashtag {
    private Long id;
    private String name;
    private Boolean isSelected;
    private OffsetDateTime createdAt;
    private Long groupId;
}
