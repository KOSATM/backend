package com.example.demo.planner.blog;

import java.util.List;

import lombok.Data;

@Data
public class BlogSearchResponse {
    private String lastBuildDate;
    private int total;
    private int start;
    private int display;
    private List<BlogItem> items; // 검색된 블로그 목록
}
