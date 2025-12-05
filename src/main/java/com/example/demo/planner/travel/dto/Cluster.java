package com.example.demo.planner.travel.dto;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Cluster {

    private int id;

    // 중심값
    private double centerLat;
    private double centerLng;

    // 소속된 장소들
    private List<ClusterPlace> places = new ArrayList<>();

    public int size() {
        return places.size();
    }
}