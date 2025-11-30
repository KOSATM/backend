package com.example.demo.supporter.map.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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

    @DeleteMapping()
    public ResponseEntity<Integer> delete() {
        return ResponseEntity.ok(service.deleteAll());
    }
}