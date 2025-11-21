package com.example.demo.dto.reviewHashtag;

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
