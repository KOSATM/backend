package com.example.demo.travelgram.aiReview.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.example.demo.travelgram.aiReview.dao.AiReviewDao;
import com.example.demo.travelgram.aiReview.dto.entity.AiReviewAnalysis;
import com.example.demo.travelgram.aiReview.dto.entity.AiReviewHashtag;
import com.example.demo.travelgram.aiReview.dto.entity.AiReviewStyle;
import com.example.demo.travelgram.aiReview.dto.request.ReviewStyleSelectRequest;
import com.example.demo.travelgram.aiReview.dto.response.AiReviewStyleResponse;
import com.example.demo.travelgram.review.dto.entity.ReviewPost;
import com.example.demo.travelgram.review.dto.response.ReviewPostResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Service
public class AiReviewService {
    private final AiReviewDao aiReviewDao;

        // 1) AI 스타일 생성
    public List<AiReviewStyleResponse> generateAiStyles(Long postId) {

        // AI 호출 → AiReviewAnalysis, AiReviewStyle(4개), AiReviewHashtag(각 3개) 저장
        AiReviewAnalysis analysis = new AiReviewAnalysis(postId);
        aiReviewDao.insertAiReview(analysis);

        List<AiReviewStyleResponse> styles = new ArrayList<>();

        for (int i = 0; i < 4; i++) {
            AiReviewStyle style = aiReviewDao.generateStyle(analysis.getId());
            aiReviewDao.insertAiReviewStyle(style);

            // 태그 3개 생성/저장 >> 대표 태그를 3개 보여주기만 하고 나머지 태그도 그거랑 비슷한거로 10~20개 추천해주면 되지 않나?
            List<String> tags = aiReviewDao.generateHashtags(style.getId());
            for (String tag : tags) {
                aiReviewDao.insertAiReviewHashtag(new AiReviewHashtag(style.getId(), tag));
            }

            styles.add(new AiReviewStyleResponse(style, tags));
        }

        return styles;
    }

    // 2) 스타일 선택 + AI 캡션 기본 입력
    public ReviewPostResponse applyStyle(Long postId, ReviewStyleSelectRequest req) {

        ReviewPost post = reviewPostDao.findById(postId);

        AiReviewStyle style = aiReviewDao.findStyleById(req.getStyleId());
        post.setStyleId(req.getStyleId());
        post.setContent(style.getGeneratedCaption()); // AI 캡션 바로 세팅

        reviewPostDao.update(post);

        return new ReviewPostResponse(postId, generatePostUrl(postId));
    }
}
