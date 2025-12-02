package com.example.demo.travelgram.aiReview.service;

import org.springframework.stereotype.Service;

import com.example.demo.travelgram.aiReview.dao.AiReviewDao;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Service
public class AiReviewService {
    private final AiReviewDao aiReviewDao;
}
