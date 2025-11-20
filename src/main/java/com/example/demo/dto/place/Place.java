package com.example.demo.dto.place;

import lombok.Data;

@Data
public class Place {
    private Long id;
    private String name;
    private String description;
    private java.math.BigDecimal lat;
    private java.math.BigDecimal lng;
    private String address;
    private String placeType;
    private String imageUrl;
    private String thumbnailUrl;
}
