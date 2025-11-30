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
        @RequestParam Double northEastLat,
        @RequestParam Double northEastLng,
        @RequestParam Double southWestLat,
        @RequestParam Double southWestLng
    ) {
        List<Toilet> toilets = service.findToiletsInBounds(northEastLat, northEastLng, southWestLat, southWestLng);
        return ResponseEntity.ok(toilets);
    }

    @GetMapping("nearest")
    public ResponseEntity<List<Toilet>> getNearestToilets(  
        @RequestParam Double userLat,
        @RequestParam Double userLng,
        @RequestParam(defaultValue = "3") Integer limit
    ) {
        List<Toilet> toilets = service.findNearestToilets(userLat, userLng, limit);
        return ResponseEntity.ok(toilets);
    }
}