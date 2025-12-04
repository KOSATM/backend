// package com.example.demo.planner.travel.service;

// import java.util.ArrayList;
// import java.util.Collections;
// import java.util.List;
// import java.util.Optional;

// import org.springframework.stereotype.Service;


// import com.example.demo.planner.travel.dto.TravelPlaceCandidate;
// import com.example.demo.planner.travel.dto.response.DayPlanResult;
// import com.example.demo.planner.travel.strategy.DayRequirement;
// import com.example.demo.planner.travel.strategy.TravelPlanStrategy;
// import com.example.demo.planner.travel.utils.CategoryNames;

// @Service
// public class DaySplitService {

//     private final TravelPlanStrategy strategy;

//     public DaySplitService(TravelPlanStrategy strategy) {
//         this.strategy = strategy;
//     }

//     /**
//      * 하루 일정 생성
//      */
//     public DayPlanResult createDayPlan(
//             int day,
//             int duration,
//             List<ClusterResult> clusters,
//             List<TravelPlaceCandidate> noise
//     ) {
//         DayRequirement req = strategy.getDayRequirement(day, duration);
//         DayPlanResult plan = new DayPlanResult(day);

//         // 전체 후보 : 클러스터 + noise 합침
//         List<TravelPlaceCandidate> allCandidates = mergeCandidates(clusters, noise);

//         // STEP 1 : SPOT 필수 채우기
//         fillCategory(plan, allCandidates, CategoryNames.SPOT, req.getMinSpot());

//         // STEP 2 : FOOD 필수 채우기
//         fillCategory(plan, allCandidates, CategoryNames.FOOD, req.getMinFood());

//         // STEP 3 : OPTIONAL 랜덤 1개 채우기
//         fillOptionalRandom(plan, allCandidates, req.getMinOptional());

//         return plan;
//     }

//     /** 후보 합치기 */
//     private List<TravelPlaceCandidate> mergeCandidates(
//             List<ClusterResult> clusters,
//             List<TravelPlaceCandidate> noise) {

//         List<TravelPlaceCandidate> list = new ArrayList<>();
//         for (ClusterResult cl : clusters) {
//             list.addAll(cl.getPlaces());
//         }
//         list.addAll(noise);
//         return list;
//     }

//     /** 필수 카테고리 채우기 */
//     private void fillCategory(
//             DayPlanResult plan,
//             List<TravelPlaceCandidate> candidates,
//             String category,
//             int requiredCount) {

//         if (requiredCount <= 0) return;

//         for (TravelPlaceCandidate c : candidates) {
//             if (plan.countByCategory(category) >= requiredCount) return;

//             if (c.getNormalizedCategory().equals(category)) {
//                 plan.addPlace(c);
//             }
//         }
//     }

//     /** 선택 카테고리 랜덤 채우기 */
//     private void fillOptionalRandom(
//             DayPlanResult plan,
//             List<TravelPlaceCandidate> candidates,
//             int requiredCount) {

//         if (requiredCount <= 0) return;

//         // 이미 채웠으면 패스
//         if (plan.countOptional() >= requiredCount) return;

//         // Optional 목록에서 랜덤 카테고리 선택
//         List<String> optionalCats = new ArrayList<>(CategoryNames.OPTIONAL);
//         Collections.shuffle(optionalCats);

//         for (String cat : optionalCats) {
//             Optional<TravelPlaceCandidate> pick = candidates.stream()
//                     .filter(c -> c.getNormalizedCategory().equals(cat))
//                     .findAny();

//             if (pick.isPresent()) {
//                 plan.addPlace(pick.get());
//                 return;
//             }
//         }
//     }
// }
