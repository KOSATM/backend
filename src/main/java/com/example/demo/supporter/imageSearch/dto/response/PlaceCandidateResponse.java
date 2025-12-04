package com.example.demo.supporter.imageSearch.dto.response;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PlaceCandidateResponse {
    @NotBlank
    private String name;
    @NotBlank
    private String type; // "poi" | "category"
    @NotBlank
    private String address;
    @NotNull
    private double lat; // 위도
    @NotNull
    private double lng; // 경도
    @NotBlank
    private String location;
    @NotBlank
    private String visualFeatures; //step1 요소와 관련된 특징
    @NotBlank
    private String similarity;
    @NotNull
    private double confidence;
    // @NotBlank
    private String imageUrl;
}
