package com.example.demo.travelgram.review.dto.entity;

import java.time.OffsetDateTime;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class ReviewPhoto {
    private Long id;
    private Long groupId;
    private String fileUrl;
    private String originalName;
    private String storedName;
    private Integer orderIndex;

    // private double lat;
    // private double lng;
    
    // private OffsetDateTime takenAt; // TZ 명시 필요, 서버에서 강제 변환 로직이용, KST로 변경해서 사용
    private OffsetDateTime createdAt;
}
