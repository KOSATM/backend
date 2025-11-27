package com.example.demo.supporter.map.apiclient;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component //이 클래스는 스프링이 관리해야 하는 bean이다.
@RequiredArgsConstructor //lombok의 기능으로 클래스 안에 있는 final 필드에게 자동으로 생성사 주입
public class ToiletApiClient {

    private static final String BASE_URL = "http://openapi.seoul.go.kr:8088";
    private static final String API_KEY  = "45464d7371646c773638507259746e";
    private static final String SERVICE_NAME = "mgisToiletPoi";
    private static final int PAGE_SIZE = 1000;
    
    private int getTotalCount() throws Exception {
        StringBuilder urlBuilder = new StringBuilder("http://openapi.seoul.go.kr:8088");
        /*URL*/
        urlBuilder.append("/" + URLEncoder.encode("45464d7371646c773638507259746e", "UTF-8"));
        /*인증키 (sample사용시에는 호출시 제한됩니다.)*/
        urlBuilder.append("/" + URLEncoder.encode("json", "UTF-8"));
        /*요청파일타입 (xml,xmlf,xls,json) */
        urlBuilder.append("/" + URLEncoder.encode("mgisToiletPoi", "UTF-8"));
        /*서비스명 (대소문자 구분 필수입니다.)*/
        urlBuilder.append("/" + URLEncoder.encode("1", "UTF-8"));
        /*요청시작위치 (sample인증키 사용시 5이내 숫자)*/
        urlBuilder.append("/" + URLEncoder.encode("5", "UTF-8"));
        /*요청종료위치(sample인증키 사용시 5이상 숫자 선택 안 됨)*/
        // 상위 5개는 필수적으로 순서바꾸지 않고 호출해야 합니다.

        URL url = new URL(urlBuilder.toString());
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Content-type", "application/xml"); //json으로 변경해야 하는지 확인
        // System.out.println("Response code: " + conn.getResponseCode()); /* 연결 자체에 대한 확인이 필요하므로 추가합니다.*/
        BufferedReader rd;

        // 서비스코드가 정상이면 200~300사이의 숫자가 나옵니다.
        if (conn.getResponseCode() >= 200 && conn.getResponseCode() <= 300) {
            rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        } else {
            rd = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
        }
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = rd.readLine()) != null) {
            sb.append(line);
        }
        rd.close();
        conn.disconnect();
        // System.out.println(sb.toString());

        String response = sb.toString();
        int startIndex = response.indexOf("\"list_total_count\":");
        int endIndex = response.indexOf(",", startIndex);
        String countStr = response.substring(startIndex + 19, endIndex).trim();

        return Integer.parseInt(countStr);
    }
    
    /**
     * start ~ end 전체 데이터 조회 (1000개씩 끊어서 호출)
    */
    public String fetchAllData() throws Exception {
        StringBuilder totalResult = new StringBuilder();

        int start = 1;
        int end = getTotalCount();
        
        for (int i = start; i <= end; i += PAGE_SIZE) {

            int pageStart = i;
            int pageEnd   = Math.min(i + PAGE_SIZE - 1, end);

            StringBuilder urlBuilder = new StringBuilder(BASE_URL);
            urlBuilder.append("/" + URLEncoder.encode(API_KEY, "UTF-8"));
            urlBuilder.append("/" + URLEncoder.encode("json", "UTF-8"));
            urlBuilder.append("/" + URLEncoder.encode(SERVICE_NAME, "UTF-8"));
            urlBuilder.append("/" + URLEncoder.encode(String.valueOf(pageStart), "UTF-8"));
            urlBuilder.append("/" + URLEncoder.encode(String.valueOf(pageEnd), "UTF-8"));

            URL url = new URL(urlBuilder.toString());
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Content-type", "application/json");

            BufferedReader rd;

            if(conn.getResponseCode() >= 200 && conn.getResponseCode() <= 300) {
                rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            } else {
                rd = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
            }

            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = rd.readLine()) != null) {
                sb.append(line);
            }

            totalResult.append(sb);

            rd.close();
            conn.disconnect();
        }

        return totalResult.toString();
    }
}
