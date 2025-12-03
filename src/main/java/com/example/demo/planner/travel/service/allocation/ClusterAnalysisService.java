package com.example.demo.planner.travel.service.allocation;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.example.demo.planner.travel.dto.response.ClusterResult;
import com.example.demo.planner.travel.service.DayPlanAllocator.ClusterInfo;
import com.example.demo.planner.travel.utils.CategoryUtils;

@Service
public class ClusterAnalysisService {

    /** 클러스터 전체 분석 */
    public List<ClusterInfo> analyzeClusters(List<ClusterResult> clusters) {
        return clusters.stream()
                .map(c -> new ClusterInfo(
                        c.getPlaces(),
                        CategoryUtils.countByCategory(c.getPlaces()),
                        false
                ))
                .collect(Collectors.toList());
    }
}