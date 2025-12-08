package com.example.demo.planner.recommendation;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.demo.common.tools.NaverInternetSearchTool;

// 패키지명은 본인 프로젝트에 맞게 수정

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NaverSearchService {

    private final NaverInternetSearchTool naverInternetSearchTool;

    /**
     * 블로그 검색 후 영어로 번역된 결과를 반환
     */
    public List<BlogItem> searchBlog(String keyword) {
        // Tool 안에 있는 getBlogInfo 메서드가 '검색 -> 태그제거 -> 번역'을 다 해줍니다.
        return naverInternetSearchTool.getBlogInfo(keyword);
    }
}
