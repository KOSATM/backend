package com.example.demo.dto.reviewPhoto;

import java.time.OffsetDateTime;
import lombok.Data;

@Data
public class RReviewPhoto {
    private Long id;
    private String fileUrl;
    private double lat;
    private double lng;
    private OffsetDateTime takenAt; // TZ 명시 필요, 서버에서 강제 변환 로직이용, KST로 변경해서 사용
    private Integer orderIndex;
    private Long groupId;
}
