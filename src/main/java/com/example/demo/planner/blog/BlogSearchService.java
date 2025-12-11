package com.example.demo.planner.blog; // 패키지명 확인

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.demo.common.tools.NaverInternetSearchTool; // 우리가 만든 툴 import

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BlogSearchService {

    // 우리가 만든 '검색+번역' 만능 도구를 주입받습니다.
    private final NaverInternetSearchTool naverInternetSearchTool;

    /**
     * 블로그 검색 후 영어로 번역된 결과를 반환
     */
    public List<BlogItem> searchBlog(String keyword) {
        // Tool 안에 있는 getBlogInfo 메서드가 '검색 -> 태그제거 -> 번역'을 다 해줍니다.
        return naverInternetSearchTool.getBlogInfo(keyword);
    }
}