package com.example.demo.supporter.imageSearch.dto.request;


import com.example.demo.supporter.imageSearch.dto.entity.ActionType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PlaceCandidateRequest {
    //Place 정보
    @NotBlank
    private String name;
    @NotBlank
    private String address;
    @NotNull
    private double lat; // 위도
    @NotNull
    private double lng; // 경도
    @NotBlank
    private String placeType;
    @NotBlank
    private String visualFeatures; //step1 요소와 관련된 특징
    @NotBlank
    private String imageUrl;
    @NotNull
    private String imageStatus; //PENDING, READY, FAILED

    //Candidate 정보
    @NotBlank
    private Boolean isSelected;
    @NotBlank
    private Integer rank; //1,2,3

    //Session 정보
    @NotNull
    private String actionType;
}
