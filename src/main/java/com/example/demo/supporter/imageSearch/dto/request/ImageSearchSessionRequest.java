package com.example.demo.supporter.imageSearch.dto.request;

import java.util.List;

import com.example.demo.supporter.imageSearch.dto.entity.ActionType;

public class ImageSearchSessionRequest {
    private Long userId;
    private ActionType actionType; // "save", "replace", "add"
    private List<PlaceCandidateRequest> candidates;
}
