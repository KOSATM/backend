package com.example.demo.supporter.imageSearch.dto.response;

import java.time.OffsetDateTime;
import java.util.List;

import com.example.demo.supporter.imageSearch.dto.entity.ActionType;

import lombok.Data;

@Data
public class SessionWithCandidatesResponse {
    private Long sessionId;
    private Long userId;
    private OffsetDateTime createdAt;
    private ActionType actionType;
    private List<CandidateWithPlaceResponse> candidates;
}
