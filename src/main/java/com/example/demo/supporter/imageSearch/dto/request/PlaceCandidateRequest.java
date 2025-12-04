package com.example.demo.supporter.imageSearch.dto.request;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PlaceCandidateRequest {
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
}
