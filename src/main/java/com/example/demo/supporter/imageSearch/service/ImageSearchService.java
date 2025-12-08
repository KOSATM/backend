package com.example.demo.supporter.imageSearch.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.common.naver.dto.LocalItem;
import com.example.demo.common.tools.NaverInternetSearchTool;
import com.example.demo.common.travel.dao.TravelPlaceDao;
import com.example.demo.supporter.imageSearch.agent.ImageSearchAgent;
import com.example.demo.supporter.imageSearch.dao.ImagePlaceDao;
import com.example.demo.supporter.imageSearch.dao.ImageSearchCandidateDao;
import com.example.demo.supporter.imageSearch.dao.ImageSearchSessionDao;
import com.example.demo.supporter.imageSearch.dto.entity.ActionType;
import com.example.demo.supporter.imageSearch.dto.entity.ImageSearchCandidate;
import com.example.demo.supporter.imageSearch.dto.request.PlaceCandidateRequest;
import com.example.demo.supporter.imageSearch.dto.response.CandidateWithPlaceResponse;
import com.example.demo.supporter.imageSearch.dto.response.ImagePlaceResponse;
import com.example.demo.supporter.imageSearch.dto.response.ImageSearchCandidateResponse;
import com.example.demo.supporter.imageSearch.dto.response.ImageSearchSessionResponse;
import com.example.demo.supporter.imageSearch.dto.response.PlaceCandidateResponse;
import com.example.demo.supporter.imageSearch.dto.response.SessionWithCandidatesResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class ImageSearchService {

    private final ImageSearchAgent agent;
    private final NaverInternetSearchTool internetSearchTool;
    private final TravelPlaceDao travelPlaceDao;
    private final ImageProcessorService imageProcessorService;
    private final ImagePlaceDao imagePlaceDao;
    private final ImageSearchSessionDao sessionDao;
    private final ImageSearchCandidateDao candidateDao;

    public List<PlaceCandidateResponse> processImageForPlaceRecommendation(String placeType, MultipartFile image,
            String address)
            throws Exception {

        // 후보자 3개 추출
        List<PlaceCandidateResponse> candidates = agent.searchImagePlace(placeType, image.getBytes(),
                image.getContentType(), address);
        // 후보자에 맞는 이미지 가지고 오기
        getCandidateImageUrl(candidates);
        return candidates;

        /*
         * List<PlaceCandidate> candidates = new ArrayList<>();
         * 
         * PlaceCandidate candidate1 = new PlaceCandidate();
         * candidate1.setName("행복한우동가게 가락점");
         * candidate1.setAddress("서울 종로구 사직로 161");
         * candidate1.setType("poi");
         * candidate1.setLocation("서울 종로구");
         * candidate1.setVisualFeatures("전통 한옥, 궁궐, 넓은 마당");
         * candidate1.setSimilarity("high");
         * candidate1.setConfidence(0.9);
         * candidate1.setImageUrl(null); // getCandidateImageUrl 메서드가 채울 필드
         * candidates.add(candidate1);
         * 
         * // 후보 2: DB에 없을 수 있는 일반 장소
         * PlaceCandidate candidate2 = new PlaceCandidate();
         * candidate2.setName("수동상회 가락시장");
         * candidate2.setAddress("서울 송파구 올림픽로 424");
         * candidate2.setType("poi");
         * candidate2.setLocation("서울 송파구");
         * candidate2.setVisualFeatures("넓은 공원, 조형물, 나무");
         * candidate2.setSimilarity("medium");
         * candidate2.setConfidence(0.8);
         * candidate2.setImageUrl(null);
         * candidates.add(candidate2);
         * 
         * getCandidateImageUrl(candidates);
         * return candidates;
         */
    }

    // 이미지 단순 확인이 아닌, 검사가 이뤄져야 함 (imageUrl인지 검색Url인지)
    private void getCandidateImageUrl(List<PlaceCandidateResponse> candidates) {
        // db안에 이미지가 있는지 확인
        for (int i = 0; i < candidates.size(); i++) {
            PlaceCandidateResponse currCandidate = candidates.get(i);
            String candidateName = currCandidate.getPlaceName();

            String imageUrlFromDB = getImageUrlFromDatabase(candidateName);

            if (imageUrlFromDB != null) {
                // 있으면 db에서 이미지 삽입
                currCandidate.setImageUrl(imageUrlFromDB);
            } else {
                // 없으면 인터넷에서 이미지 검색
                String imageUrlFromInternet = searchImageFromInternet(candidateName);
                if (imageUrlFromInternet != null) {
                    currCandidate.setImageUrl(imageUrlFromInternet);
                }
            }
            log.info("후보자 {}의 이미지 URL: {}", i + 1, currCandidate.getImageUrl());
        }
    }

    @Transactional(readOnly = true)
    private String getImageUrlFromDatabase(String candidateName) {
        return travelPlaceDao.selectImgUrlByTitle(candidateName);
    }

    private String searchImageFromInternet(String candidateName) {
        final int MAX_ATTEMPTS = 3;
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            try {
                String imageUrlFromInternet = internetSearchTool.getImgUrl(candidateName);

                // URL이 유효한 링크인지 확인
                if (isDirectImageUrl(imageUrlFromInternet)) {
                    return imageUrlFromInternet;
                }
            } catch (Exception e) {
                log.warn("인터넷 이미지 검색 중 예외 발생: {}", e.getMessage());
            }
        }
        return null;
    }

    public boolean isDirectImageUrl(String url) {
        if (url == null || url.isEmpty()) {
            return false;
        }
        String lowercaseUrl = url.split("\\?")[0].toLowerCase();

        return lowercaseUrl.endsWith("jpg") ||
                lowercaseUrl.endsWith("jpeg") ||
                lowercaseUrl.endsWith("png") ||
                lowercaseUrl.endsWith("gif") ||
                lowercaseUrl.endsWith("webp");
    }

    // @Transactional
    public Long savePlaceCandidates(Long userId, List<PlaceCandidateRequest> candidates) {
        ImageSearchSessionResponse session = new ImageSearchSessionResponse();
        session.setUserId(userId);
        session.setActionType(ActionType.valueOf(candidates.get(0).getActionType()));
        sessionDao.insert(session);
        Long sessionId = session.getId();
        log.info("ImageSearchSession 저장 완료, ID: {}", sessionId);

        //각 Candidate에 대해 place 생성 -> candidate 생성
        for (int i = 0; i < candidates.size(); i++) {
            PlaceCandidateRequest candidate = candidates.get(i);

            // 경도 / 위도 저장
            LocalItem localItem = internetSearchTool.getLocalInfo(candidate.getName()).get(0);

            // 네이버 api에서 제공하는 위/경도 값은 좌표를 1,000,000배 확대한 값
            candidate.setLat(Double.parseDouble(localItem.getMapy()) / 10_000_000.0);
            candidate.setLng(Double.parseDouble(localItem.getMapx()) / 10_000_000.0);

            log.info("위도: {}", candidate.getLat());
            log.info("경도: {}", candidate.getLng());

            // 원본 이미지 S3 업로드 및 URL 획득
            ImageProcessorService.ImageUrlResult imageUrls = imageProcessorService
                    .processAndStoreImageFromUrl(candidate);

            ImagePlaceResponse placeEntity = toEntity(candidate, imageUrls);
            imagePlaceDao.save(placeEntity);
            log.info("ImagePlace 저장 완료, ID: {}", placeEntity.getId());

            //candidate 저장
            ImageSearchCandidateResponse candidateResponse = new ImageSearchCandidateResponse();
            candidateResponse.setImageSearchSessionId(sessionId);
            candidateResponse.setImagePlaceId(placeEntity.getId());
            candidateResponse.setIsSelected(candidate.getIsSelected());
            candidateResponse.setRank(Long.valueOf(candidate.getRank()));

            candidateDao.insert(candidateResponse);
            log.info("ImageSearchCandidate 저장 완료, ID: {}", candidateResponse.getId());
        }

        return sessionId;
    }

    private ImagePlaceResponse toEntity(PlaceCandidateRequest candidate,
            ImageProcessorService.ImageUrlResult imageUrls) {
        ImagePlaceResponse response = new ImagePlaceResponse();
        response.setName(candidate.getName());
        response.setDescription(candidate.getVisualFeatures());
        response.setLat(candidate.getLat());
        response.setLng(candidate.getLng());
        response.setAddress(candidate.getAddress());

        response.setPlaceType(candidate.getPlaceType());

        if (imageUrls != null) {
            response.setInternalOriginalUrl(imageUrls.originalUrl());
            response.setInternalThumbnailUrl(imageUrls.thumbnailUrl());
        }
        response.setExternalImageUrl(candidate.getImageUrl());

        String statusString = candidate.getImageStatus();
        try {
            ImagePlaceResponse.ImageStatusEnum statusEnum = ImagePlaceResponse.ImageStatusEnum.valueOf(statusString);
            response.setImageStatus(statusEnum);
        } catch (IllegalArgumentException e) {
            log.warn("잘못된 ImageStatus 문자열입니다 : {}. FAILED로 설정합니다.", statusString);
            response.setImageStatus(ImagePlaceResponse.ImageStatusEnum.FAILED);
        }
        return response;
    }

    @Transactional
    public int updateSessionActionType(Long candidateId, String actionType) {
        // 1. Candidate ID로 Candidate 조회
        ImageSearchCandidate candidate = candidateDao.selectById(candidateId);
        if (candidate == null) {
            log.warn("Candidate를 찾을 수 없습니다. candidateId: {}", candidateId);
            return 0;
        }
        
        // 2. Session ID 추출
        Long sessionId = candidate.getImageSearchSessionId();
        
        // 3. Session의 ActionType 업데이트
        int result = sessionDao.updateActionType(sessionId, actionType);
        log.info("Session ActionType 업데이트 완료. sessionId: {}, actionType: {}", sessionId, actionType);
        
        return result;
    }

    @Transactional
    public int delete(Long candidateId) {
        //Candidate ID로 Candidate 조회
        ImageSearchCandidate candidate = candidateDao.selectById(candidateId);
        if (candidate == null) {
            log.warn("Candidate를 찾을 수 없습니다. candidateId: {}", candidateId);
            return 0;
        }

        Long sessionId = candidate.getImageSearchSessionId();
        Long placeId = candidate.getImagePlaceId();
        
        // 1. Candidate 삭제 (외래키 참조 제거)
        int deleted = candidateDao.delete(candidateId);
        log.info("Candidate 삭제 완료, candidateId: {}", candidateId);
        
        // 2. 연결된 Place 삭제
        imagePlaceDao.deleteById(placeId);
        log.info("Place 삭제 완료, placeId: {}", placeId);

        // 3. 
        java.util.List<ImageSearchCandidate> remainingCandidates = candidateDao.selectBySessionId(sessionId);
        if (remainingCandidates.isEmpty()) {
            sessionDao.delete(sessionId);
            log.info("Session 삭제 완료, sessionId: {}", sessionId);
        }

        return deleted;
    }

    @Transactional(readOnly = true)
    public java.util.List<SessionWithCandidatesResponse> getSessionsByUserId(Long userId) {
        // 1. userId로 모든 Session 조회
        java.util.List<ImageSearchSessionResponse> sessions = sessionDao.selectByUserId(userId);
        
        // 2. 각 Session에 대해 Candidate + Place 정보 조회
        java.util.List<SessionWithCandidatesResponse> result = new java.util.ArrayList<>();
        for (ImageSearchSessionResponse session : sessions) {
            SessionWithCandidatesResponse sessionWithCandidates = new SessionWithCandidatesResponse();
            sessionWithCandidates.setSessionId(session.getId());
            sessionWithCandidates.setUserId(session.getUserId());
            sessionWithCandidates.setCreatedAt(session.getCreatedAt());
            sessionWithCandidates.setActionType(session.getActionType());
            
            // Candidate + Place 조회 (JOIN)
            java.util.List<CandidateWithPlaceResponse> candidates = candidateDao.selectWithPlaceBySessionId(session.getId());
            sessionWithCandidates.setCandidates(candidates);
            
            result.add(sessionWithCandidates);
        }
        return result;
    }
}