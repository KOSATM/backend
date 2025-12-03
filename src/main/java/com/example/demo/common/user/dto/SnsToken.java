package com.example.demo.common.user.dto;

import java.time.OffsetDateTime;
import lombok.Data;

@Data
public class SnsToken {
    private Long id;
    private String userId;
    private String userAccessToken;
    private String pageAccessToken;
    private OffsetDateTime expiresAt;
    private String accountType;
    private String igBusinessAccount;
    private String creatorAccount;
    private String publishAccount;
    private OffsetDateTime createdAt;
}
