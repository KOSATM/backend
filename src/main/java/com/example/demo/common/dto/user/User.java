package com.example.demo.common.dto.user;

import java.time.OffsetDateTime;
import lombok.Data;

/**
 * 사용자 엔티티
 * 시스템의 사용자 정보를 나타냅니다.
 */
@Data
public class User {
    /** 사용자 ID */
    private Long id;
    
    /** 활성 상태 여부 */
    private Boolean isActive;
    
    /** 마지막 로그인 시각 */
    private OffsetDateTime lastLoginAt;
    
    /** 사용자 이름 */
    private String name;
    
    /** 이메일 주소 */
    private String email;
    
    /** 프로필 이미지 URL */
    private String profileImageUrl;
    
    /** 생성 시각 */
    private OffsetDateTime createdAt;
    
    /** 수정 시각 */
    private OffsetDateTime updatedAt;
}
