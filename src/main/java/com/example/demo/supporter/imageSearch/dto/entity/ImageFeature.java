package com.example.demo.supporter.imageSearch.dto.entity;

import lombok.Data;

@Data
public class ImageFeature {
    private String name;
    private String type; // "poi" | "category"
    private String visualFeatures;
    private double confidence;
}
