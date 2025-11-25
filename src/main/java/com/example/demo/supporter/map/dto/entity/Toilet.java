package com.example.demo.supporter.map.dto.entity;

import lombok.Data;

@Data
public class Toilet {
    private Long id;
    private String name;
    private double lat;
    private double lng;
    private String address;
}
