package com.example.demo.supporter.dto;

import lombok.Data;

@Data
public class Place {
    private Long id;
    private String name;
    private String description;
    private double lat;
    private double lng;
    private String address;
    private String placeType;
    private String imageUrl;
    private String thumbnailUrl;
}
