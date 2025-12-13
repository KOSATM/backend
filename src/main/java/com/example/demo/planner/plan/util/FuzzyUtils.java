package com.example.demo.planner.plan.util;

import java.util.List;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Fuzzy Matching 유틸리티
 * - 문자열 유사도 비교
 * - 정규화
 * - Levenshtein 거리 계산
 */
@Component
@Slf4j
public class FuzzyUtils {

    /**
     * 사용자 입력과 가장 유사한 장소명 찾기 (Fuzzy Matching)
     *
     * 1. 정확히 일치하는 경우
     * 2. 대소문자 무시하고 일치
     * 3. 공백/특수문자 제거 후 일치
     * 4. 부분 문자열 포함 (contains)
     * 5. Levenshtein 거리가 가장 가까운 것
     *
     * @param userInput 사용자 입력 문자열
     * @param placeNames 후보 장소명 리스트
     * @return 가장 유사한 장소명, 없으면 null
     */
    public String findClosestPlaceName(String userInput, List<String> placeNames) {
        if (userInput == null || placeNames == null || placeNames.isEmpty()) {
            return null;
        }

        String normalizedInput = normalizeForMatching(userInput);
        String bestMatch = null;
        int bestScore = Integer.MAX_VALUE;  // Levenshtein 거리 (낮을수록 좋음)
        int bestMatchLevel = 10;            // 매칭 우선순위 (낮을수록 좋음)

        for (String candidate : placeNames) {
            if (candidate == null) continue;

            // 1. 정확히 일치
            if (candidate.equals(userInput)) {
                log.info("Exact match found: '{}'", candidate);
                return candidate;
            }

            // 2. 대소문자 무시
            if (candidate.equalsIgnoreCase(userInput)) {
                if (bestMatchLevel > 2) {
                    bestMatchLevel = 2;
                    bestMatch = candidate;
                    log.info("Case-insensitive match: '{}'", candidate);
                }
                continue;
            }

            // 3. 정규화 후 비교
            String normalizedCandidate = normalizeForMatching(candidate);
            if (normalizedCandidate.equals(normalizedInput)) {
                if (bestMatchLevel > 3) {
                    bestMatchLevel = 3;
                    bestMatch = candidate;
                    log.info("Normalized match: '{}' → '{}'", userInput, candidate);
                }
                continue;
            }

            // 4. 부분 문자열 포함
            if (candidate.contains(userInput) || userInput.contains(candidate)) {
                if (bestMatchLevel > 4) {
                    bestMatchLevel = 4;
                    bestMatch = candidate;
                    log.info("Substring match: '{}' ↔ '{}'", userInput, candidate);
                }
                continue;
            }

            // 정규화된 문자열에서 부분 일치
            if (normalizedCandidate.contains(normalizedInput) || normalizedInput.contains(normalizedCandidate)) {
                if (bestMatchLevel > 5) {
                    bestMatchLevel = 5;
                    bestMatch = candidate;
                    log.info("Normalized substring match: '{}' ↔ '{}'", userInput, candidate);
                }
                continue;
            }

            // 5. Levenshtein 거리 (최소 거리 찾기)
            int distance = levenshteinDistance(normalizedInput, normalizedCandidate);
            if (distance < bestScore || (distance == bestScore && bestMatchLevel > 6)) {
                bestScore = distance;
                bestMatch = candidate;
                bestMatchLevel = 6;
            }
        }

        if (bestMatch != null) {
            log.info("Fuzzy match result: '{}' → '{}' (level={}, distance={})",
                    userInput, bestMatch, bestMatchLevel, bestScore);
        } else {
            log.warn("No match found for: '{}'", userInput);
        }

        return bestMatch;
    }

    /**
     * 문자열 정규화 (매칭용)
     * - 소문자 변환
     * - 공백, 특수문자 제거
     * - 한글 초성/중성/종성 분리 없이 그대로 유지
     */
    public String normalizeForMatching(String input) {
        if (input == null) return "";

        return input
            .toLowerCase()
            .replaceAll("[\\s\\-_()\\[\\]{},.·•]", "")  // 공백, 특수문자 제거
            .trim();
    }

    /**
     * Levenshtein 거리 계산 (편집 거리)
     * 두 문자열이 얼마나 다른지를 정수로 반환
     *
     * @param a 문자열 A
     * @param b 문자열 B
     * @return 최소 편집 횟수 (낮을수록 유사함)
     */
    public int levenshteinDistance(String a, String b) {
        if (a == null) a = "";
        if (b == null) b = "";

        int[][] dp = new int[a.length() + 1][b.length() + 1];

        for (int i = 0; i <= a.length(); i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= b.length(); j++) {
            dp[0][j] = j;
        }

        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                if (a.charAt(i - 1) == b.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1];
                } else {
                    dp[i][j] = Math.min(
                        Math.min(dp[i - 1][j], dp[i][j - 1]),
                        dp[i - 1][j - 1]
                    ) + 1;
                }
            }
        }

        return dp[a.length()][b.length()];
    }
}
