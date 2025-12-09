package com.example.demo.planner.blog;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class BlogSearchController {

    private final BlogSearchService blogSearchService;

    @GetMapping("/blog")
    public ResponseEntity<List<BlogItem>> searchBlog(@RequestParam("keyword") String keyword) {
        // 1. 서비스 호출
        List<BlogItem> result = blogSearchService.searchBlog(keyword);
        
        // 2. 결과가 없으면 204 No Content 혹은 빈 리스트 반환
        if (result == null || result.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        // 3. 번역된 결과(JSON) 반환
        return ResponseEntity.ok(result);
    }
}
