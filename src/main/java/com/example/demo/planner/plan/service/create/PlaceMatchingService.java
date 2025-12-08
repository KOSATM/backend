package com.example.demo.planner.plan.service.create;

import java.util.List;

import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

// 장소명 Fuzzy Matching 서비스 (한글/영어 혼용, 띄어쓰기 무시, Levenshtein distance 기반)
@Service
@Slf4j
public class PlaceMatchingService {

    // 가장 유사한 장소명 찾기 (최소 40% 유사도 이상)
    public String findClosestPlaceName(String userInput, List<String> placeNames) {
        if (userInput == null || userInput.isEmpty() || placeNames.isEmpty()) {
            return null;
        }

        String normalizedInput = normalizeForMatching(userInput);

        String bestMatch = null;
        int bestScore = Integer.MAX_VALUE;
        double bestSimilarity = 0.0;

        for (String placeName : placeNames) {
            String normalizedPlace = normalizeForMatching(placeName);

            // 1. 완전 일치 체크 (최우선)
            if (normalizedPlace.equals(normalizedInput)) {
                return placeName;
            }

            // 2. 부분 일치 체크 (높은 우선순위)
            if (normalizedPlace.contains(normalizedInput)) {
                int score = normalizedPlace.length() - normalizedInput.length();
                if (score < bestScore) {
                    bestScore = score;
                    bestMatch = placeName;
                    bestSimilarity = 1.0;
                }
                continue;
            }

            if (normalizedInput.contains(normalizedPlace)) {
                int score = normalizedInput.length() - normalizedPlace.length();
                if (score < bestScore) {
                    bestScore = score;
                    bestMatch = placeName;
                    bestSimilarity = 0.9;
                }
                continue;
            }

            // 3. Levenshtein distance 계산
            int distance = levenshteinDistance(normalizedInput, normalizedPlace);
            double similarity = 1.0 - ((double) distance
                    / Math.max(normalizedInput.length(), normalizedPlace.length()));

            // 유사도가 60% 이상이고, 이전 best보다 좋으면 업데이트
            if (similarity >= 0.6 && (bestMatch == null || similarity > bestSimilarity
                    || (similarity == bestSimilarity && distance < bestScore))) {
                bestScore = distance;
                bestMatch = placeName;
                bestSimilarity = similarity;
            }
        }

        // 최소 유사도 40% 이상만 반환
        if (bestSimilarity < 0.4) {
            log.info("No match found for '{}' (best similarity: {})", userInput, bestSimilarity);
            return null;
        }

        log.info("Fuzzy match: '{}' → '{}' (similarity: {}, distance: {})",
                userInput, bestMatch, String.format("%.2f", bestSimilarity), bestScore);
        return bestMatch;
    }

    // 문자열 정규화 (소문자 변환 + 공백/특수문자 제거)
    private String normalizeForMatching(String input) {
        return input.toLowerCase()
                .replace(" ", "")
                .replace("-", "")
                .replace("_", "")
                .replace("(", "")
                .replace(")", "")
                .replace("[", "")
                .replace("]", "");
    }

    // Levenshtein Distance 계산 (두 문자열 간 편집 거리)
    private int levenshteinDistance(String a, String b) {
        int[][] dp = new int[a.length() + 1][b.length() + 1];

        for (int i = 0; i <= a.length(); i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= b.length(); j++) {
            dp[0][j] = j;
        }

        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(
                        Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                        dp[i - 1][j - 1] + cost);
            }
        }

        return dp[a.length()][b.length()];
    }
}
