package com.example.demo.planner.plan.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Plan CRUD API 통합 테스트
 * 실제 API 엔드포인트가 제대로 작동하는지 검증
 * 
 * Note: 이 테스트는 실제 데이터베이스를 사용하며, @Transactional로 각 테스트 후 데이터를 롤백합니다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional // 각 테스트 후 데이터 롤백
class PlanControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 1. Plan 생성 테스트 (CREATE)
     * POST /api/plans?userId=1&days=3
     */
    @Test
    void testCreatePlan_Success() throws Exception {
        mockMvc.perform(post("/api/plans")
                .param("userId", "1")
                .param("days", "3")
                .param("budget", "500000")
                .param("startDate", "2025-12-10"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.budget").value(500000))
                .andExpect(jsonPath("$.startDate").value("2025-12-10"))
                .andExpect(jsonPath("$.endDate").value("2025-12-12"))
                .andExpect(jsonPath("$.isEnded").value(false));
    }

    /**
     * 2. Plan 생성 테스트 - 기본값 사용
     * days, budget, startDate를 생략하면 기본값이 적용되어야 함
     */
    @Test
    void testCreatePlan_WithDefaultValues() throws Exception {
        mockMvc.perform(post("/api/plans")
                .param("userId", "1"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.budget").value(500000)) // 기본값
                .andExpect(jsonPath("$.isEnded").value(false));
    }

    /**
     * 3. Plan 단건 조회 테스트 (READ)
     * GET /api/plans/{planId}
     */
    @Test
    void testGetPlan_Success() throws Exception {
        // 먼저 Plan을 생성
        MvcResult createResult = mockMvc.perform(post("/api/plans")
                .param("userId", "1")
                .param("days", "2"))
                .andExpect(status().isCreated())
                .andReturn();

        String responseBody = createResult.getResponse().getContentAsString();
        Long planId = objectMapper.readTree(responseBody).get("id").asLong();

        // 생성된 Plan 조회
        mockMvc.perform(get("/api/plans/{planId}", planId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(planId))
                .andExpect(jsonPath("$.userId").value(1));
    }

    /**
     * 4. Plan 조회 테스트 - 존재하지 않는 Plan
     * 404 Not Found가 반환되어야 함
     */
    @Test
    void testGetPlan_NotFound() throws Exception {
        mockMvc.perform(get("/api/plans/{planId}", 99999))
                .andExpect(status().isNotFound());
    }

    /**
     * 5. Plan 상세 조회 테스트 (READ with Details)
     * GET /api/plans/{planId}/detail
     * Plan + Days + Places 모두 포함
     */
    @Test
    void testGetPlanDetail_Success() throws Exception {
        // Plan 생성 (3일)
        MvcResult createResult = mockMvc.perform(post("/api/plans")
                .param("userId", "1")
                .param("days", "3"))
                .andExpect(status().isCreated())
                .andReturn();

        String responseBody = createResult.getResponse().getContentAsString();
        Long planId = objectMapper.readTree(responseBody).get("id").asLong();

        // 상세 조회
        mockMvc.perform(get("/api/plans/{planId}/detail", planId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plan.id").value(planId))
                .andExpect(jsonPath("$.plan.userId").value(1))
                .andExpect(jsonPath("$.days").isArray())
                .andExpect(jsonPath("$.days", hasSize(3))) // 3일
                .andExpect(jsonPath("$.days[0].day.dayIndex").value(1))
                .andExpect(jsonPath("$.days[0].places", hasSize(2))) // 각 날짜마다 2개의 Place
                .andExpect(jsonPath("$.days[1].day.dayIndex").value(2))
                .andExpect(jsonPath("$.days[1].places", hasSize(2)))
                .andExpect(jsonPath("$.days[2].day.dayIndex").value(3))
                .andExpect(jsonPath("$.days[2].places", hasSize(2)));
    }

    /**
     * 6. 사용자별 Plan 목록 조회 테스트
     * GET /api/plans/user/{userId}
     */
    @Test
    void testGetPlansByUserId_Success() throws Exception {
        Long userId = 1L;

        // 동일한 userId로 2개의 Plan 생성
        mockMvc.perform(post("/api/plans")
                .param("userId", userId.toString())
                .param("days", "2"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/plans")
                .param("userId", userId.toString())
                .param("days", "3"))
                .andExpect(status().isCreated());

        // 사용자의 Plan 목록 조회
        mockMvc.perform(get("/api/plans/user/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(2))))
                .andExpect(jsonPath("$[0].userId").value(userId))
                .andExpect(jsonPath("$[1].userId").value(userId));
    }

    /**
     * 7. 사용자별 Plan 목록 조회 - Plan이 없는 사용자
     * 빈 배열이 반환되어야 함
     */
    @Test
    void testGetPlansByUserId_EmptyList() throws Exception {
        mockMvc.perform(get("/api/plans/user/{userId}", 99999))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    /**
     * 8. Plan 수정 테스트 (UPDATE)
     * 현재 미구현 - 501 Not Implemented 반환
     */
    @Test
    void testUpdatePlan_NotImplemented() throws Exception {
        mockMvc.perform(put("/api/plans/{planId}", 1)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"budget\": 600000}"))
                .andExpect(status().isNotImplemented());
    }

    /**
     * 9. Plan 삭제 테스트 (DELETE)
     * 현재 미구현 - 501 Not Implemented 반환
     */
    @Test
    void testDeletePlan_NotImplemented() throws Exception {
        mockMvc.perform(delete("/api/plans/{planId}", 1))
                .andExpect(status().isNotImplemented());
    }

    /**
     * 10. 다양한 일수로 Plan 생성 테스트
     * 1일부터 7일까지 생성 가능
     */
    @Test
    void testCreatePlan_VariousDays() throws Exception {
        // 1일 여행
        mockMvc.perform(post("/api/plans")
                .param("userId", "1")
                .param("days", "1"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.startDate").exists())
                .andExpect(jsonPath("$.endDate").exists());

        // 7일 여행
        mockMvc.perform(post("/api/plans")
                .param("userId", "1")
                .param("days", "7"))
                .andExpect(status().isCreated());
    }
}
