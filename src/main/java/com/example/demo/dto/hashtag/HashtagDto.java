package com.example.demo.dto.hashtag;

import java.time.OffsetDateTime;
import lombok.Data;

@Data
public class HashtagDto {
    private Long id;
    private String name;
    private Boolean isSelected;
    private OffsetDateTime createdAt;
    private Long groupId;
}
