package com.example.demo.supporter.map.service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.example.demo.supporter.map.apiclient.ToiletApiClient;
import com.example.demo.supporter.map.dao.ToiletDao;
import com.example.demo.supporter.map.dto.entity.Toilet;

@Service
@Slf4j
@RequiredArgsConstructor
public class ToiletService {

	private final ToiletApiClient apiClient;
	private final ToiletDao dao;
	private final Toilet toilet;

	/**
	 * 데이터를 전체 삭제하고 다시 전체 API로 채움
	 */
	public void refreshToiletData() throws Exception {
		// 1. 기존 데이터 삭제
		dao.deleteAll();

		// 2. API로 전체 데이터 조회
		String jsonData = apiClient.fetchAllData();

		parseAndSaveToilets(jsonData);
	}

	private void parseAndSaveToilets(String response) {
		// row 배열 추철
		int rowStart = response.indexOf("\"row\":[");
		int rowEnd = response.lastIndexOf("]");

		if (rowStart != -1 && rowEnd != -1) {
			String rowData = response.substring(rowStart + 7, rowEnd);
			String[] items = rowData.split("\\},\\{");

			long startTime = System.currentTimeMillis();
			int count = 0;

			for (String item : items) {
				item = item.replace("{", "").replace("}", "");
				Map<String, String> toilet = extractToiletData(item);

				if (saveToiletToDB(toilet)) {
					count++;
				}
			}
			long endTime = System.currentTimeMillis();
			System.out.println(count + "개 데이터 저장 완료 (" + (endTime - startTime) + "ms)");
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

	private boolean saveToiletToDB(Map<String, String> toilet) {
		try {
			// db에 저장
			Toilet dbToilet = new Toilet();
			dbToilet.setAddress(toilet.get("ADDR_NEW"));
			dbToilet.setLat(Double.parseDouble(toilet.get("COORD_Y")));
			dbToilet.setLng(Double.parseDouble(toilet.get("COORD_X")));
			dbToilet.setName(toilet.get("CONTS_NAME"));
			
			dao.insert(dbToilet);
			return true;
		} catch (Exception e) {
			System.err.println("DB 저장 오류: " + e.getMessage());
			return false;
		}
	}

	public Toilet get(Long id) {
		return dao.selectById(id);
	}

	public List<Toilet> getAll() {
		return dao.selectAll();
	}

	@Transactional
	public Long create(Toilet t) {
		dao.insert(t);
		return t.getId();
	}

	@Transactional
	public int update(Toilet t) {
		return dao.update(t);
	}

	// 기존 데이터 모두 삭제
	@Transactional
	public int deleteAll() {
		return dao.deleteAll();
	}

}