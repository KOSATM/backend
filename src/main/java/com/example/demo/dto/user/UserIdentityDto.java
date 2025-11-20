package com.example.demo.dto.user;

import java.time.OffsetDateTime;
import lombok.Data;

@Data
public class UserIdentityDto {
    private Long id;
    private Long userId;
    private String provider;
    private OffsetDateTime createdAt;
}
