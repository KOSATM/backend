package com.example.demo.supporter.checklist.controller;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.example.demo.supporter.checklist.dto.entity.ChecklistItem;
import com.example.demo.supporter.checklist.service.ChecklistItemService;

@RestController
@RequestMapping("/supporter/checklist-items")
@RequiredArgsConstructor
public class ChecklistItemController {

    private final ChecklistItemService service;

    @GetMapping("/{id}")
    public ResponseEntity<ChecklistItem> get(@PathVariable Long id) {
        return ResponseEntity.ok(service.get(id));
    }

    @GetMapping("/checklist/{checklistId}")
    public ResponseEntity<List<ChecklistItem>> list(@PathVariable Long checklistId) {
        return ResponseEntity.ok(service.getAll(checklistId));
    }

    @PostMapping
    public ResponseEntity<Long> create(@RequestBody ChecklistItem item) {
        return ResponseEntity.ok(service.create(item));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Integer> update(@PathVariable Long id, @RequestBody ChecklistItem item) {
        item.setId(id);
        return ResponseEntity.ok(service.update(item));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Integer> delete(@PathVariable Long id) {
        return ResponseEntity.ok(service.delete(id));
    }

    // userId로 모든 체크리스트 항목 조회
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<ChecklistItem>> listByUserId(@PathVariable("userId") Long userId) {
        return ResponseEntity.ok(service.getAllByUserId(userId));
    }
}