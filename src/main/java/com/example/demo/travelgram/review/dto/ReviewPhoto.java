package com.example.demo.travelgram.review.dto;

import java.time.OffsetDateTime;
import lombok.Data;

@Data
public class ReviewPhoto {
    private Long id;
    private String fileUrl;
    private double lat;
    private double lng;
    private OffsetDateTime takenAt; // TZ 명시 필요, 서버에서 강제 변환 로직이용, KST로 변경해서 사용
    private Integer orderIndex;
    private OffsetDateTime createdAt;
    private Long groupId;
}
