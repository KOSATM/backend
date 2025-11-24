package com.example.demo.supporter.dto;

import lombok.Data;

@Data
public class Toilet {
    private Long id;
    private String name;
    private double lat;
    private double lng;
    private String address;
}
