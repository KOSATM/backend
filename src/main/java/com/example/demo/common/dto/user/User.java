package com.example.demo.common.dto.user;

import java.time.OffsetDateTime;
import lombok.Data;

@Data
public class User {
    private Long id;
    private Boolean isActive;
    private OffsetDateTime lastLoginAt;
    private String name;
    private String email;
    private String profileImageUrl;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
