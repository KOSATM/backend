package com.example.demo.planner.plan.dto;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class ClusterBundle {

    private List<Cluster> clusters = new ArrayList<>();

    public void addCluster(Cluster cluster) {
        clusters.add(cluster);
    }

    public Cluster get(int idx) {
        return clusters.get(idx);
    }
}