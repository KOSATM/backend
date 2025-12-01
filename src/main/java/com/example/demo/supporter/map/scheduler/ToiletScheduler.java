package com.example.demo.supporter.map.scheduler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.example.demo.supporter.map.apiclient.ToiletApiClient;
import com.example.demo.supporter.map.service.ToiletService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class ToiletScheduler {

	@Autowired
	private ToiletApiClient apiClient;
	@Autowired
    private ToiletService toiletService;
	
	@Scheduled(cron = "0 0 0 1 * *")
	public void refreshToiletData() throws Exception {
		log.info("Toilet 데이터 업데이트 시작");
        try {
            toiletService.refreshToiletData();
            log.info("Toilet 데이터 업데이트 완료");
        } catch (Exception e) {
            log.error("Toilet 데이터 업데이트 실패", e);
        }
	}
}
