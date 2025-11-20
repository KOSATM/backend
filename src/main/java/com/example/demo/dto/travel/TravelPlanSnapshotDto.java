package com.example.demo.dto.travel;

import java.time.OffsetDateTime;
import lombok.Data;

@Data
public class TravelPlanSnapshotDto {
    private Long id;
    private Long userId;
    private Integer versionNo;
    private String snapshotJson;
    private OffsetDateTime createdAt;
}
