package com.example.demo.supporter.map.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.supporter.map.dto.entity.Toilet;
import com.example.demo.supporter.map.service.ToiletService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/supporter/toilets")
@RequiredArgsConstructor
public class ToiletController {
    private final ToiletService service;

    //테스트용, 데이터 갱신 API
    @PostMapping()
    public ResponseEntity<Integer> create() throws Exception {
        service.refreshToiletData();
        return ResponseEntity.ok().build();
    }

    @GetMapping("in-bounds")
    public ResponseEntity<List<Toilet>> getToiletsInBounds(
        @RequestParam("northEastLat") Double northEastLat,
        @RequestParam("northEastLng") Double northEastLng,
        @RequestParam("southWestLat") Double southWestLat,
        @RequestParam("southWestLng") Double southWestLng
    ) {
        List<Toilet> toilets = service.findToiletsInBounds(northEastLat, northEastLng, southWestLat, southWestLng);
        return ResponseEntity.ok(toilets);
    }

    @GetMapping("nearest")
    public ResponseEntity<List<Toilet>> getNearestToilets(  
        @RequestParam("userLat") Double userLat,
        @RequestParam("userLng") Double userLng,
        @RequestParam(value = "limit", defaultValue = "3") Integer limit
    ) {
        List<Toilet> toilets = service.findNearestToilets(userLat, userLng, limit);
        return ResponseEntity.ok(toilets);
    }
}