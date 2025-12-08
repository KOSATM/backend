package com.example.demo.planner.recommendation;

import java.util.List;

import lombok.Data;

@Data
public class NaverBlogSearchResponse {
    private String lastBuildDate;
    private int total;
    private int start;
    private int display;
    private List<BlogItem> items; // 검색된 블로그 목록
}