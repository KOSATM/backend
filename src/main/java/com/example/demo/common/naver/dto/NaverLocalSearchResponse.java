package com.example.demo.common.naver.dto;

import java.util.List;

import lombok.Data;

@Data
public class NaverLocalSearchResponse {
    private List<LocalItem> items;
}
