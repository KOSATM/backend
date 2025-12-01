package com.example.demo.planner.travel.dto.entity;

import java.time.OffsetDateTime;
import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@NoArgsConstructor
@ToString
public class TravelPlaces {
    private Long id;
    private String contentId;
    private String title;
    private String address;
    private String tel;
    private String firstImage;
    private String firstImage2;
    private Double lat;
    private Double lng;
    private String categoryCode;
    private String description;
    private List<String> tags;
    private float[] embedding;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private String detailInfo;
    private String normalizedCategory;
}