package com.example.demo.supporter.map.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.supporter.map.apiclient.ToiletApiClient;
import com.example.demo.supporter.map.dao.ToiletDao;
import com.example.demo.supporter.map.dto.entity.Toilet;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class ToiletService {

	private final ToiletApiClient apiClient;
	private final ToiletDao dao;

	/*
	 * 데이터를 전체 삭제하고 다시 전체 API로 채움
	 */
	public void refreshToiletData() throws Exception {
		log.info("Toilet 데이터 갱신 시작");

		// 1. API로 전체 데이터 조회
		String jsonData = apiClient.fetchAllData();

		// 2. API 데이터 검증
		if (jsonData == null || jsonData.isEmpty()) {
			throw new Exception("Toilet API 데이터 조회 실패");
		}

		// 3. 파싱 가능한지 검증
		int rowStart = jsonData.indexOf("\"row\":[");
		int rowEnd = jsonData.lastIndexOf("]");
		if (rowStart == -1 || rowEnd == -1) {
			throw new Exception("Toilet API 데이터 파싱 실패");
		}

		// 4. 검증 성공 후 기존 데이터 삭제
		log.info("기존 Toilet 데이터 삭제");
		dao.deleteAll();

		// 5. 새 데이터 저장
		parseAndSaveToilets(jsonData);
		log.info("Toilet 데이터 갱신 완료");
	}

	private void parseAndSaveToilets(String response) {
		// row 배열 추철
		int rowStart = response.indexOf("\"row\":[");
		int rowEnd = response.lastIndexOf("]");

		if (rowStart != -1 && rowEnd != -1) {
			String rowData = response.substring(rowStart + 7, rowEnd);
			String[] items = rowData.split("\\},\\{");

			long startTime = System.currentTimeMillis();
			List<Toilet> toilets = new ArrayList<>();

			for (String item : items) {
				item = item.replace("{", "").replace("}", "");
				Map<String, String> toiletData = extractToiletData(item);

				Toilet dbToilet = new Toilet();
				dbToilet.setAddress(toiletData.get("ADDR_NEW"));
				dbToilet.setLat(Double.parseDouble(toiletData.get("COORD_Y")));
				dbToilet.setLng(Double.parseDouble(toiletData.get("COORD_X")));
				dbToilet.setName(toiletData.get("CONTS_NAME"));

				toilets.add(dbToilet);
			}

			dao.insertBatch(toilets);

			long endTime = System.currentTimeMillis();
			log.info("Toilet 데이터 저장 완료. 소요 시간: {} ms", (endTime - startTime));
		}
	}

	private Map<String, String> extractToiletData(String item) {
		Map<String, String> toilet = new HashMap<>();
		// 도로명 주소
		toilet.put("ADDR_NEW", extractValue(item, "ADDR_NEW"));
		// 지번 주소
		toilet.put("ADDR_OLD", extractValue(item, "ADDR_OLD"));
		// 경도 x
		toilet.put("COORD_X", extractValue(item, "COORD_X"));
		// 위도 y
		toilet.put("COORD_Y", extractValue(item, "COORD_Y"));
		// 건물 이름
		toilet.put("CONTS_NAME", extractValue(item, "CONTS_NAME"));
		return toilet;
	}

	private String extractValue(String data, String key) {
		int startIndex = data.indexOf("\"" + key + "\":\"");
		if (startIndex == -1) {
			// 숫자 값인 경우 (따옴표 없음)
			startIndex = data.indexOf("\"" + key + "\":");
			if (startIndex == -1) {
				return "";
			}
			int endIndex = data.indexOf(",", startIndex);
			if (endIndex == -1) {
				endIndex = data.length();
			}
			return data.substring(startIndex + key.length() + 3, endIndex).trim();
		}
		// 문자열 값인 경우 (따옴표 있음)
		int endIndex = data.indexOf("\"", startIndex + key.length() + 4);
		return data.substring(startIndex + key.length() + 4, endIndex);
	}

	// 기존 데이터 모두 삭제
	@Transactional
	public int deleteAll() {
		return dao.deleteAll();
	}

	// 지도 경계 내 화장실 조회
	public List<Toilet> findToiletsInBounds(Double northEastLat, Double northEastLng, Double southWestLat,
			Double southWestLng) {
		return dao.findInBounds(northEastLat, northEastLng, southWestLat, southWestLng);
	}

	//사용자 위치 기준 가장 가까운 N개 화장실 조회
	public List<Toilet> findNearestToilets(Double userLat, Double userLng, Integer limit) {
		return dao.findNearest(userLat, userLng, limit);
	}

}