package com.example.demo.common.user.dto;

import java.time.OffsetDateTime;
import lombok.Data;

@Data
public class UserIdentity {
    private Long id;
    private Long userId;
    private String provider;
    private OffsetDateTime createdAt;
}
