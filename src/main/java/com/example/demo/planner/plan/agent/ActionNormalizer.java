package com.example.demo.planner.plan.agent;

import java.util.List;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * 🔄 Action Normalizer
 *
 * LLM이 생성한 자연어 action을 내부 표준 명령어로 정규화
 *
 * 예시:
 * - "일정 조회해줘" → "view"
 * - "첫째날 일정 보여줘" → "view"
 * - "롯데리아 대신 버거킹으로 바꿔줘" → "replace"
 * - "첫번째 삭제해줘" → "delete"
 */
@Component
@Slf4j
public class ActionNormalizer {

    private final List<Rule> rules = List.of(
        new Rule("조회", "view",
            List.of("일정", "조회", "보여줘", "전체", "보기", "확인", "알려줘", "뭐야")),
        new Rule("교체", "replace",
            List.of("대신", "바꿔", "교체", "변경", "수정", "말고")),
        new Rule("추가", "add",
            List.of("추가", "넣어", "삽입", "등록")),
        new Rule("삭제", "delete",
            List.of("삭제", "빼줘", "제거", "지워")),
        new Rule("순서변경", "swap",
            List.of("서로 바꿔", "스왑", "순서", "교환"))
    );

    /**
     * 자연어 action을 내부 표준 명령어로 정규화
     * @param naturalAction LLM이 생성한 자연어 action (예: "일정 보여줘")
     * @return 정규화된 명령어 (예: "view")
     */
    public String normalize(String naturalAction) {
        if (naturalAction == null || naturalAction.isBlank()) {
            log.warn("⚠️ 빈 action 입력");
            return "unknown";
        }

        String action = naturalAction.trim().toLowerCase();
        log.debug("🔄 정규화 시도: '{}'", action);

        for (Rule rule : rules) {
            if (rule.matches(action)) {
                log.info("✅ 정규화 성공: '{}' → '{}'", naturalAction, rule.internal);
                return rule.internal;
            }
        }

        log.warn("❓ 알 수 없는 action: '{}'", naturalAction);
        return "unknown";
    }

    /**
     * 매칭 규칙
     */
    private static class Rule {
        String name;           // 규칙 이름 (설명용)
        String internal;       // 내부 명령어
        List<String> keywords; // 매칭 키워드들

        Rule(String name, String internal, List<String> keywords) {
            this.name = name;
            this.internal = internal;
            this.keywords = keywords;
        }

        /**
         * action이 이 규칙에 매칭되는지 확인
         */
        boolean matches(String action) {
            return keywords.stream()
                .anyMatch(keyword -> action.contains(keyword.toLowerCase()));
        }
    }
}
