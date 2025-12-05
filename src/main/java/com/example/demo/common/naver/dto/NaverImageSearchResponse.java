package com.example.demo.common.naver.dto;

import java.util.List;

import lombok.Data;

@Data
public class NaverImageSearchResponse {
    private List<ImageItem> items;
}
