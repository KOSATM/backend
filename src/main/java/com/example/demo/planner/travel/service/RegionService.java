package com.example.demo.planner.travel.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.example.demo.common.chat.intent.dto.SeoulRegion;
import com.example.demo.planner.travel.cluster.GeoUtils;
import com.example.demo.planner.travel.dto.TravelPlaceCandidate;

import lombok.extern.slf4j.Slf4j;


@Service
@Slf4j
public class RegionService {

    public List<TravelPlaceCandidate> applyRegionPreference(
            List<TravelPlaceCandidate> candidates,
            String regionName,
            int duration
    ) {
        if (candidates == null || candidates.isEmpty()) return candidates;

        // 지역 매칭
        SeoulRegion region = SeoulRegion.fromUserInput(regionName);
        
        // 지역 없는 경우 = 그냥 전체 서울 기준 랜덤 일정
        if (region == null) {
            return new ArrayList<>(candidates);
        }

        // 가중치 적용
        applyScoreWeight(candidates, region);

        // 반경 필터링 + fallback
        return applyRadiusWithFallback(candidates, region, duration);
    }

    private void applyScoreWeight(List<TravelPlaceCandidate> candidates, SeoulRegion region) {
        for (TravelPlaceCandidate p : candidates) {
            double d = GeoUtils.haversine(
                p.getTravelPlacesLat(),
                p.getTravelPlacesLng(),
                region.lat,
                region.lng
            );

            double bonus = 0.0;

            if (d < 3) bonus += 0.20;
            else if (d < 6) bonus += 0.10;

            String addr = p.getTravelPlaces().getAddress();
            if (addr != null) {
                for (String kw : region.keywords) {
                    if (addr.contains(kw)) {
                        bonus += 0.15;
                        break;
                    }
                }
            }

            p.setScore(p.getScore() + bonus);
        }
    }

    private List<TravelPlaceCandidate> applyRadiusWithFallback(
            List<TravelPlaceCandidate> list,
            SeoulRegion region,
            int duration
    ) {
        int min = duration * 6;

        List<TravelPlaceCandidate> r1 = filterByRadius(list, region, 6.0);
        if (r1.size() >= min) return sortAndLimit(r1);

        List<TravelPlaceCandidate> r2 = filterByRadius(list, region, 8.0);
        if (r2.size() >= min) return sortAndLimit(r2);

        return sortAndLimit(list); // fallback
    }

    private List<TravelPlaceCandidate> filterByRadius(
            List<TravelPlaceCandidate> list,
            SeoulRegion region,
            double radius
    ) {
        List<TravelPlaceCandidate> result = new ArrayList<>();
        for (TravelPlaceCandidate p : list) {
            double d = GeoUtils.haversine(
                p.getTravelPlacesLat(),
                p.getTravelPlacesLng(),
                region.lat,
                region.lng
            );
            if (d <= radius) result.add(p);
        }
        return result;
    }

    private List<TravelPlaceCandidate> sortAndLimit(List<TravelPlaceCandidate> list) {
        list.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));
        int limit = Math.min(80, list.size());
        return new ArrayList<>(list.subList(0, limit));
    }

}
