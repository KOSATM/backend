package com.example.demo.supporter.imageSearch.dto.response;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PlaceCandidateResponse {
    @NotBlank
    private String placeName;
    @NotBlank
    private String type; // "poi" | "category"
    @NotBlank
    private String address;
    // @NotNull
    // private double lat; // 위도
    // @NotNull
    // private double lng; // 경도
    @NotBlank
    private String location;
    @NotBlank
    private String association; //step1 요소와의 관계
    @NotBlank
    private String description; //장소명에 대한 간단한 설명
    @NotBlank
    private String similarity;
    @NotNull
    private double confidence;
    // @NotBlank
    private String imageUrl;
}
