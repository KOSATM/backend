package com.example.demo.supporter.imageSearch.dto.entity;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PlaceCandidate {
    @NotBlank
    private String address;
    @NotBlank
    private String name;
    @NotBlank
    private String type; // "poi" | "category"
    @NotBlank
    private String location;
    @NotBlank
    private String visualFeatures; //step1 요소와 관련된 특징
    @NotNull
    private double similarity;
    @NotNull
    private double confidence;
}
