package com.example.demo.dto.place;

import lombok.Data;

@Data
public class ToiletDto {
    private Long id;
    private String name;
    private java.math.BigDecimal latitude;
    private java.math.BigDecimal longitude;
    private String address;
}
