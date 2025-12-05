package com.example.demo.planner.travel.strategy;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class DayRequirement {
    private int minFood;
    private int maxFood;
    private int minSpot;
    private int minOptional;
    private int maxPlaces;
    
}