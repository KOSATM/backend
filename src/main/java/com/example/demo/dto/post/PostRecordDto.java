package com.example.demo.dto.post;

import java.time.OffsetDateTime;
import lombok.Data;

@Data
public class PostRecordDto {
    private Long id;
    private String caption;
    private Boolean isPosted;
    private String postUrl;
    private OffsetDateTime createdAt;
    private Long travelPlanId;
    private Long styleId;
}
