package com.example.demo.dto.photo;

import java.time.OffsetDateTime;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class Photo {
    private Long id;
    private String fileUrl;
    private BigDecimal lat;
    private BigDecimal lng;
    private OffsetDateTime takenAt;
    private Integer orderIndex;
    private OffsetDateTime createdAt;
    private Long groupId;
}
