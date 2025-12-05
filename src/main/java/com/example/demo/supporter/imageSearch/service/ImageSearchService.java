package com.example.demo.supporter.imageSearch.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.common.tools.InternetSearchTool;
import com.example.demo.common.travel.dao.TravelPlaceDao;
import com.example.demo.supporter.imageSearch.agent.ImageSearchAgent;
import com.example.demo.supporter.imageSearch.dao.ImagePlaceDao;
import com.example.demo.supporter.imageSearch.dto.request.PlaceCandidateRequest;
import com.example.demo.supporter.imageSearch.dto.response.ImagePlaceResponse;
import com.example.demo.supporter.imageSearch.dto.response.PlaceCandidateResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class ImageSearchService {

    private final ImageSearchAgent agent;
    private final InternetSearchTool internetSearchTool;
    private final TravelPlaceDao travelPlaceDao;
    private final ImageProcessorService imageProcessorService;
    private final ImagePlaceDao imagePlaceDao;
    
    @Transactional
    public List<PlaceCandidateResponse> processImageForPlaceRecommendation(String placeType, MultipartFile image, String address)
            throws Exception {

        // 후보자 3개 추출
        List<PlaceCandidateResponse> candidates = agent.searchImagePlace(placeType, image.getBytes(), image.getContentType(), address);
        // 후보자에 맞는 이미지 가지고 오기
        getCandidateImageUrl(candidates);
        return candidates;

        /*
        List<PlaceCandidate> candidates = new ArrayList<>();

        PlaceCandidate candidate1 = new PlaceCandidate();
        candidate1.setName("행복한우동가게 가락점");
        candidate1.setAddress("서울 종로구 사직로 161");
        candidate1.setType("poi");
        candidate1.setLocation("서울 종로구");
        candidate1.setVisualFeatures("전통 한옥, 궁궐, 넓은 마당");
        candidate1.setSimilarity("high");
        candidate1.setConfidence(0.9);
        candidate1.setImageUrl(null); // getCandidateImageUrl 메서드가 채울 필드
        candidates.add(candidate1);

        // 후보 2: DB에 없을 수 있는 일반 장소
        PlaceCandidate candidate2 = new PlaceCandidate();
        candidate2.setName("수동상회 가락시장");
        candidate2.setAddress("서울 송파구 올림픽로 424");
        candidate2.setType("poi");
        candidate2.setLocation("서울 송파구");
        candidate2.setVisualFeatures("넓은 공원, 조형물, 나무");
        candidate2.setSimilarity("medium");
        candidate2.setConfidence(0.8);
        candidate2.setImageUrl(null);
        candidates.add(candidate2);

        getCandidateImageUrl(candidates);
        return candidates;
        */
    }

    private void getCandidateImageUrl(List<PlaceCandidateResponse> candidates) {
        //db안에 이미지가 있는지 확인
        for (int i = 0; i < candidates.size(); i++) {
            PlaceCandidateResponse currCandidate = candidates.get(i);
            String candidateName = currCandidate.getName();

            String imageUrlFromDB = travelPlaceDao.selectImgUrlByTitle(candidateName);
            if (imageUrlFromDB != null) {
                //있으면 db에서 이미지 삽입
                currCandidate.setImageUrl(imageUrlFromDB);
            } else {
            //없으면 인터넷 서치를 이용해 이미지 삽입
                String imageUrlFromInternet = internetSearchTool.getImgUrl(candidateName);
                currCandidate.setImageUrl(imageUrlFromInternet
                );
            }
            log.info("후보자 {}의 이미지 URL: {}", i + 1, currCandidate.getImageUrl());
        }
    }

    @Transactional
    public void savePlaceCandidates(List<PlaceCandidateRequest> candidates) {
        for (PlaceCandidateRequest candidate : candidates) {
            
            //원본 이미지 S3 업로드 및 URL 획득
            ImageProcessorService.ImageUrlResult imageUrls = imageProcessorService.processAndStoreImageFromUrl(candidate);

            // DTO를 Entity로 변환
            ImagePlaceResponse entity = toEntity(candidate, imageUrls);

            //DAO를 통해 DB에 저장
            imagePlaceDao.save(entity);
        }

    }

    private ImagePlaceResponse toEntity(PlaceCandidateRequest candidate, ImageProcessorService.ImageUrlResult imageUrls) {
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
}