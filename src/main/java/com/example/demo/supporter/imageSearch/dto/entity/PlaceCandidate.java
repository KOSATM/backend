package com.example.demo.supporter.imageSearch.dto.entity;

import lombok.Data;

@Data
public class PlaceCandidate {
    private String name;
    private String type; // "poi" | "category"
    private String location;
    private String visualFeatures; //step1 요소와 관련된 특징
    private String similarity;
    private double confidence;
}
