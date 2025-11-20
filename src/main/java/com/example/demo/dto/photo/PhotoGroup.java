package com.example.demo.dto.photo;

import java.time.OffsetDateTime;
import lombok.Data;

@Data
public class PhotoGroup {
    private Long id;
    private OffsetDateTime createdAt;
    private Long postRecordId;
}
