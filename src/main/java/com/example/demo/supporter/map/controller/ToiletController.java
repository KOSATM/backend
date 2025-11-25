package com.example.demo.supporter.map.controller;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.example.demo.supporter.map.dto.entity.Toilet;
import com.example.demo.supporter.map.service.ToiletService;

@RestController
@RequestMapping("/supporter/toilets")
@RequiredArgsConstructor
public class ToiletController {
    private final ToiletService service;

    @GetMapping("/{id}")
    public ResponseEntity<Toilet> get(@PathVariable Long id) {
        return ResponseEntity.ok(service.get(id));
    }

    @GetMapping
    public ResponseEntity<List<Toilet>> list() {
        return ResponseEntity.ok(service.getAll());
    }

    @PostMapping
    public ResponseEntity<Long> create(@RequestBody Toilet t) {
        return ResponseEntity.ok(service.create(t));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Integer> update(@PathVariable Long id, @RequestBody Toilet t) {
        t.setId(id);
        return ResponseEntity.ok(service.update(t));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Integer> delete(@PathVariable Long id) {
        return ResponseEntity.ok(service.delete(id));
    }
}