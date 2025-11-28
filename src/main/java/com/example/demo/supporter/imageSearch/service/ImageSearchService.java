package com.example.demo.supporter.imageSearch.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.supporter.imageSearch.agent.ImageSearchAgent;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ImageSearchService {

    private final ImageSearchAgent agent;

    @Transactional
    public Long processImageForPlaceRecommendation(String placeType, MultipartFile image) throws Exception {

        agent.analyzeImage(
                placeType,
                image.getContentType(),
                image.getBytes());
        return null;
    }

}