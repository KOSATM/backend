package com.example.demo.dto.reviewHashtag;

import java.time.OffsetDateTime;
import lombok.Data;

@Data
public class ReviewHashtagGroup {
    private Long id;
    private OffsetDateTime createdAt;
    private Long reviewPostId;
}
