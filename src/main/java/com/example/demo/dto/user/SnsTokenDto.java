package com.example.demo.dto.user;

import java.time.OffsetDateTime;
import lombok.Data;

@Data
public class SnsTokenDto {
    private Long id;
    private String accessToken;
    private String refreshToken;
    private OffsetDateTime expiresAt;
    private String accountType;
    private String igBusinessId;
    private OffsetDateTime createdAt;
    private String userId;
}
