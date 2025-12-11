package com.example.demo.planner.plan.agent;

import java.util.*;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import com.example.demo.common.chat.intent.dto.IntentCommand;
import com.example.demo.common.chat.pipeline.AiAgentResponse;
import com.example.demo.common.global.agent.AiAgent;
import com.example.demo.planner.plan.dto.context.PlanContext;
import com.example.demo.planner.plan.dto.entity.Plan;
import com.example.demo.planner.plan.service.action.PlanActionExecutor;
import com.example.demo.planner.plan.service.create.PlanService;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class SmartPlanAgent implements AiAgent {

    private final ChatClient chatClient;
    private final PlanService planService;
    private final PlanActionExecutor planActionExecutor;
    private final Map<Long, List<String>> historyMap = new HashMap<>();

    public SmartPlanAgent(ChatClient.Builder builder, PlanService planService, PlanActionExecutor executor) {
        this.chatClient = builder.build();
        this.planService = planService;
        this.planActionExecutor = executor;
    }

    @Override
    public AiAgentResponse execute(IntentCommand command, Long userId) {

        String userMsg = command.getOriginalUserMessage();
        log.info("[SmartPlanAgent] User({}): {}", userId, userMsg);

        PlanContext ctx = loadContext(userId);
        if (!ctx.hasActivePlan()) return AiAgentResponse.of("현재 활성화된 여행 일정이 없습니다.");

        String planJson = ctx.toJson();
        List<String> history = historyMap.computeIfAbsent(userId, k -> new ArrayList<>());
        history.add("User: " + userMsg);

        String systemPrompt = buildSystemPrompt();
        String userPrompt = buildUserPrompt(planJson, history, userMsg);

        String llm = chatClient.prompt().system(systemPrompt).user(userPrompt).call().content();
        log.info("[LLM Response]\n{}", llm);

        String clean = llm.replaceAll("```\\s*", "").trim();
        return clean.startsWith("FUNCTION_CALL:")
                ? handleFunctionCall(clean, ctx, userId, systemPrompt)
                : AiAgentResponse.of(llm);
    }

    /* ─────────────────────────────────────────────
     * FUNCTION_CALL 처리
     * ───────────────────────────────────────────── */
    private AiAgentResponse handleFunctionCall(
            String clean, PlanContext ctx, Long userId, String systemPrompt
    ) {
        String[] parts = clean.split("\n", 2);
        String callLine = parts[0].replace("FUNCTION_CALL:", "").trim();
        String naturalMsg = parts.length > 1 ? parts[1].trim() : "";

        String result = executeFunctionCall(callLine, ctx.getActivePlan().getId());
        log.info("[Function Result] {}", result);

        // 성공 → 최신 일정 로드 + LLM 확인 프롬프트
        if (result.startsWith("✅")) {
            PlanContext updated = loadContext(userId);
            String confirmPrompt = """
                    Function 실행 결과:
                    %s

                    변경된 일정:
                    %s

                    사용자에게 자연스럽게 안내해주세요.
                    """.formatted(result, updated.toJson());

            String answer = chatClient.prompt().system(systemPrompt).user(confirmPrompt).call().content();
            saveHistory(userId, answer);
            return AiAgentResponse.of(answer);
        }

        // 검색 결과 또는 실패
        String answer = result + (!naturalMsg.isEmpty() ? "\n\n" + naturalMsg : "");
        saveHistory(userId, answer);
        return AiAgentResponse.of(answer);
    }

    /* ─────────────────────────────────────────────
     * Function Call 파싱 + 실행
     * ───────────────────────────────────────────── */
    private String executeFunctionCall(String line, Long planId) {
        try {
            int idx = line.indexOf('(');
            if (idx == -1) return "❌ Function Call 형식 오류";

            String fn = line.substring(0, idx).trim();
            String args = line.substring(idx + 1, line.lastIndexOf(')')).trim();

            Map<String, String> params = parseArgs(args);
            return dispatch(fn, params, planId);

        } catch (Exception e) {
            log.error("Function Call 처리 오류", e);
            return "❌ Function Call 처리 중 오류: " + e.getMessage();
        }
    }

    private Map<String, String> parseArgs(String argsStr) {
        Map<String, String> map = new HashMap<>();
        if (argsStr.isEmpty()) return map;

        for (String pair : argsStr.split(",")) {
            String[] kv = pair.split("=", 2);
            if (kv.length != 2) continue;

            String key = normalizeKey(kv[0].trim());
            String value = kv[1].trim().replaceAll("^\"|\"$", "");

            if (!value.isBlank() && !"null".equalsIgnoreCase(value)) {
                map.put(key, value);
            }
        }
        return map;
    }

    /* ─────────────────────────────────────────────
     * 디스패처
     * ───────────────────────────────────────────── */
    private String dispatch(String fn, Map<String, String> p, Long pid) {
        try {
            return switch (fn) {
                case "deletePlace" -> planActionExecutor.deletePlace(pid, p.get("placeName"));
                case "swapPlaces" -> planActionExecutor.swapPlaces(pid,
                        i(p, "dayIndex"), i(p, "index1"), i(p, "index2"));
                case "swapPlacesBetweenDays" -> planActionExecutor.swapPlacesBetweenDays(pid,
                        i(p, "day1"), i(p, "index1"), i(p, "day2"), i(p, "index2"));
                case "replacePlace" -> planActionExecutor.replacePlace(pid,
                        p.get("oldPlaceName"), p.get("newPlaceName"));
                case "searchPlace" -> planActionExecutor.searchPlace(p.get("searchQuery"));
                case "replacePlaceWithSelection" -> planActionExecutor.replacePlaceWithSelection(pid,
                        p.get("oldPlaceName"), p.get("newPlaceName"), i(p, "selectedIndex"));
                case "addPlace" -> planActionExecutor.addPlace(pid,
                        i(p, "dayIndex"), p.get("placeName"), p.get("startTime"));
                case "addPlaceAtPosition" -> planActionExecutor.addPlaceAtPosition(pid,
                        i(p, "dayIndex"), i(p, "position"), p.get("placeName"),
                        p.containsKey("duration") ? i(p, "duration") : null);
                case "updatePlaceTime" -> planActionExecutor.updatePlaceTime(pid,
                        p.get("placeName"), p.get("newTime"));
                case "deleteDay" -> planActionExecutor.deleteDay(pid, i(p, "dayIndex"));
                case "swapDays" -> planActionExecutor.swapDays(pid,
                        i(p, "day1"), i(p, "day2"));
                case "extendPlan" -> planActionExecutor.extendPlan(pid, i(p, "extraDays"));
                case "deletePlan" -> planActionExecutor.deletePlan(pid);

                default -> "❌ 알 수 없는 함수: " + fn;
            };
        } catch (Exception e) {
            return "❌ Function 실행 중 오류: " + e.getMessage();
        }
    }

    private int i(Map<String, String> p, String k) { return Integer.parseInt(p.get(k)); }

    private String normalizeKey(String k) {
        return switch (k) {
            case "old", "oldName", "oldPlace" -> "oldPlaceName";
            case "new", "newName", "newPlace" -> "newPlaceName";
            case "day", "dayIdx" -> "dayIndex";
            case "idx" -> "index";
            default -> k;
        };
    }

    /* ─────────────────────────────────────────────
     * Prompt Builder
     * ───────────────────────────────────────────── */
    private String buildUserPrompt(String json, List<String> history, String userMsg) {
        String hist = history.size() > 20
                ? String.join("\n", history.subList(history.size() - 20, history.size()))
                : String.join("\n", history);

        return """
                ### 전체 여행 일정 (JSON):
                ```json
                %s
                ```

                ### 지금까지의 대화:
                %s

                ### 사용자 요청:
                "%s"
                """.formatted(json, hist, userMsg);
    }

    private void saveHistory(Long userId, String answer) {
        historyMap.get(userId).add("Assistant: " + answer);
    }

    private PlanContext loadContext(Long userId) {
        try {
            Plan plan = planService.findActiveByUserId(userId);
            return (plan == null)
                    ? PlanContext.empty()
                    : PlanContext.builder()
                            .activePlan(plan)
                            .allDays(planService.queryAllDaysOptimized(plan.getId()))
                            .build();
        } catch (Exception e) {
            return PlanContext.empty();
        }
    }

    private String buildSystemPrompt() {
        return """
        당신은 여행 일정 관리 AI 입니다. 사용자의 요청을 보고 필요하면 FUNCTION_CALL을 사용하여 일정을 수정하세요.
        FUNCTION_CALL 형식:
        FUNCTION_CALL: 함수명(key="value")
        """;
    }
}
