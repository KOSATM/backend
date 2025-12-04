package com.example.demo.common.user.dto;

import java.time.OffsetDateTime;
import lombok.Data;

// 사용자 엔티티
// 시스템의 사용자 정보를 나타냅니다.
@Data
public class User {
    private Long id; // 사용자 ID
    private Boolean isActive; // 활성 상태 여부
    private OffsetDateTime lastLoginAt; // 마지막 로그인 시각
    private String name; // 사용자 이름
    private String email; // 이메일 주소
    private String profileImageUrl; // 프로필 이미지 URL
    private OffsetDateTime createdAt; // 생성 시각
    private OffsetDateTime updatedAt; // 수정 시각
}
