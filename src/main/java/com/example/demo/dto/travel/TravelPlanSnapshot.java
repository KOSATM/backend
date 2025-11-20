package com.example.demo.dto.travel;

import java.time.OffsetDateTime;
import lombok.Data;

@Data
public class TravelPlanSnapshot {
    private Long id;
    private Long userId;
    private Integer versionNo;
    private String snapshotJson;
    private OffsetDateTime createdAt;
}
