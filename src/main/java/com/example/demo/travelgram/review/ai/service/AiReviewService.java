package com.example.demo.travelgram.review.ai.service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.planner.plan.dao.PlanDao;
import com.example.demo.planner.plan.dao.PlanDayDao;
import com.example.demo.planner.plan.dao.PlanPlaceDao;
import com.example.demo.planner.plan.dto.entity.Plan;
import com.example.demo.planner.plan.dto.entity.PlanDay;
import com.example.demo.planner.plan.dto.entity.PlanPlace;
import com.example.demo.travelgram.review.ai.agent.PlanTitleGenerateAgent;
import com.example.demo.travelgram.review.ai.agent.ReviewStyleGenerateAgent;
import com.example.demo.travelgram.review.ai.builder.ReviewInputJsonBuilder;
import com.example.demo.travelgram.review.ai.dao.AiReviewDao;
import com.example.demo.travelgram.review.ai.dto.entity.AiReviewAnalysis;
import com.example.demo.travelgram.review.ai.dto.entity.AiReviewHashtag;
import com.example.demo.travelgram.review.ai.dto.entity.AiReviewStyle;
import com.example.demo.travelgram.review.ai.dto.response.AiReviewStyleResponse;
import com.example.demo.travelgram.review.ai.dto.response.GeneratedStyleResponse;
import com.example.demo.travelgram.review.dao.ReviewPhotoDao;
import com.example.demo.travelgram.review.dao.ReviewPostDao;
import com.example.demo.travelgram.review.dto.entity.ReviewPost;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Service
public class AiReviewService {
    private final PlanDao planDao;
    private final PlanDayDao dayDao;
    private final PlanPlaceDao placeDao;

    private final ReviewPhotoDao photoDao;
    private final ReviewInputJsonBuilder builder;

    private final PlanTitleGenerateAgent planTitleGenerateAgent;

    private final AiReviewDao aiReviewDao;

    private final ReviewStyleGenerateAgent styleAgent; // 추가 주입
    private final ReviewPostDao reviewPostDao; // 추가 주입
    private final ObjectMapper objectMapper; // 추가 주입

    public ObjectNode createPlanInputJson(Long planId) {
        // 🟦 1) plan 전체 조회
        Plan plan = planDao.selectPlanById(planId);

        // 🟦 2) days 조회
        List<PlanDay> planDays = dayDao.selectPlanDaysByPlanId(planId);

        // 🟦 3) map<Long, List<PlanPlace>> 형태로 정리
        Map<Long, List<PlanPlace>> placesByDayId = new HashMap<>();

        for (PlanDay day : planDays) {
            List<PlanPlace> places = placeDao.selectPlanPlacesByPlanDayId(day.getId());
            placesByDayId.put(day.getId(), places);
        }

        // 🟦 4) builder 호출해서 JsonNode 생성
        return builder.build(plan, planDays, placesByDayId);

    }

    public String generatePlanTitle(Long planId) {
        ObjectNode inputJson = createPlanInputJson(planId);

        // LLM에게 보내기 쉽게 String으로 변환
        String inputJsonString = inputJson.toPrettyString();
        // Title을 agent 통해 생성
        String title = planTitleGenerateAgent.generatePlanTitle(inputJsonString);

        return title;
    }

    /**
     * AI 리뷰 스타일 생성 및 저장 (메인 로직)
     */
    @Transactional
    public List<AiReviewStyleResponse> createAndSaveStyles(Long planId, Long reviewPostId) {
        
        // 1. 여행 데이터 JSON 생성 (기존 Builder 활용)
        ObjectNode inputNode = createPlanInputJson(planId);
        String inputJson = inputNode.toPrettyString();

        // 2. ReviewPost에서 Mood, Type 조회
        // (ReviewPostDao에 selectById가 있다고 가정하거나 추가 필요)
        ReviewPost post = reviewPostDao.selectReviewPostById(reviewPostId);
        if (post == null)
            throw new IllegalArgumentException("Review Post not found");

        String mood = post.getOverallMoods();
        String type = post.getTravelType();

        // 3. Agent 호출 (AI 생성)
        GeneratedStyleResponse aiResponse = styleAgent.generateStyles(inputJson, mood, type);

        // 4. 분석 이력 저장 (AiReviewAnalysis)
        // output_json은 나중에 디버깅용으로 AI 전체 응답을 저장
        String outputJsonString = "";
        try {
            outputJsonString = objectMapper.writeValueAsString(aiResponse);
        } catch (Exception e) {
        }

        AiReviewAnalysis analysis = AiReviewAnalysis.builder()
                .reviewPostId(reviewPostId)
                .inputJson(inputJson)
                .outputJson(outputJsonString)
                .build();

        aiReviewDao.insertAiReviewAnalysis(analysis); // id 생성됨

        List<AiReviewStyleResponse> resultList = new ArrayList<>();
        // 5. Save Styles & Hashtags
        for (GeneratedStyleResponse.StyleItem item : aiResponse.getStyles()) {
            
            // 5-1. Save Style
            AiReviewStyle style = AiReviewStyle.builder()
                    .reviewAnalysisId(analysis.getId())
                    .name(item.getToneName()) 
                    .toneCode(item.getToneCode())
                    .createdAt(OffsetDateTime.now())
                    .caption(item.getCaption()) // Make sure this matches your DB column
                    .build();
            
            aiReviewDao.insertAiReviewStyle(style); 

            // 5-2. Save Hashtags
            List<AiReviewHashtag> savedHashtags = new ArrayList<>();
            for (String tagName : item.getHashtags()) {
                String cleanTagName = tagName.replace("#", "");
                AiReviewHashtag tag = AiReviewHashtag.builder()
                        .reviewStyleId(style.getId())
                        .name(cleanTagName)
                        .createdAt(OffsetDateTime.now())
                        .build();
                aiReviewDao.insertAiReviewHashtag(tag);
                savedHashtags.add(tag);
            }

            // 5-3. Add to Result List
            resultList.add(new AiReviewStyleResponse(style, savedHashtags));
        }
        
        return resultList;
    }
}
