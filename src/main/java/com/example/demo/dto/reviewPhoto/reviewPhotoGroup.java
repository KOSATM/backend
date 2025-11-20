package com.example.demo.dto.reviewPhoto;

import java.time.OffsetDateTime;
import lombok.Data;

@Data
public class reviewPhotoGroup {
    private Long id;
    private OffsetDateTime createdAt;
    private Long reviewPostId;
}
