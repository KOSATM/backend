package com.example.demo.dto.photo;

import java.time.OffsetDateTime;
import lombok.Data;

@Data
public class PhotoGroupDto {
    private Long id;
    private OffsetDateTime createdAt;
    private Long postRecordId;
}
