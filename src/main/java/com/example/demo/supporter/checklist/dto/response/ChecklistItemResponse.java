package com.example.demo.supporter.checklist.dto.response;

import java.util.List;

import lombok.Data;

@Data
public class ChecklistItemResponse {
    private String title;
    private List<String> items;
}
