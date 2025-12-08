package com.example.demo.planner.recommendation;

import lombok.Data;

@Data
public class BlogItem {
    private String title;       // 블로그 글 제목
    private String link;        // 블로그 글 링크
    private String description; // 글 요약
    private String bloggername; // 블로거 이름
    private String bloggerlink; // 블로거 링크
    private String postdate;    // 작성일
}