package com.example.demo.supporter.imageSearch.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.common.tools.InternetSearchTool;
import com.example.demo.common.travel.dao.TravelPlaceDao;
import com.example.demo.supporter.imageSearch.agent.ImageSearchAgent;
import com.example.demo.supporter.imageSearch.dto.entity.PlaceCandidate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class ImageSearchService {

    private final ImageSearchAgent agent;
    private final InternetSearchTool internetSearchTool;
    private final TravelPlaceDao travelPlaceDao;

    @Transactional
    public Long processImageForPlaceRecommendation(String placeType, MultipartFile image, String address)
            throws Exception {

        // 후보자 3개 추출
        List<PlaceCandidate> candidates = agent.searchImagePlace(placeType, image.getBytes(), image.getContentType(),
                address);
        // 후보자에 맞는 이미지 가지고 오기
        getCandidateImageUrl(candidates);

        return null;
    }

    private void getCandidateImageUrl(List<PlaceCandidate> candidates) {
        //db안에 이미지가 있는지 확인
        for (int i = 0; i < candidates.size(); i++) {
            if (travelPlaceDao.selectImgUrlByTitle(candidates.get(i).getName()) != null) {
                //있으면 db에서 이미지 삽입
                candidates.get(i).setImageUrl(
                travelPlaceDao.selectImgUrlByTitle(candidates.get(i).getName())
                );
                
            } else {
            //없으면 인터넷 서치를 이용해 이미지 삽입
                candidates.get(i).setImageUrl(
                internetSearchTool.getImgUrl(candidates.get(i).getName())
                );
            }
            log.info("후보자 {}의 이미지 URL: {}", i + 1, candidates.get(i).getImageUrl());
        }
    }
}