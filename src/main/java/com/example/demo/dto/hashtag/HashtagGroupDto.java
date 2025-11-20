package com.example.demo.dto.hashtag;

import java.time.OffsetDateTime;
import lombok.Data;

@Data
public class HashtagGroupDto {
    private Long id;
    private OffsetDateTime createdAt;
    private Long postRecordId;
}
