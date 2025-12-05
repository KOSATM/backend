package com.example.demo.common.chat.intent.dto;

import java.util.List;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum SeoulRegion {

    JONGNO(37.5730, 126.9794, List.of("종로", "종로구", "광화문", "혜화", "청운동")),
    JUNG(37.5633, 126.9978, List.of("중구", "명동", "을지로", "소공동", "장충동")),
    YONGSAN(37.5326, 126.9905, List.of("용산", "용산구", "이태원", "한남", "삼각지")),
    SEONGDONG(37.5635, 127.0364, List.of("성동", "성수", "뚝섬", "서울숲", "왕십리")),
    GWANGJIN(37.5384, 127.0822, List.of("광진", "건대", "자양", "구의동")),
    DONGDAEMUN(37.5744, 127.0396, List.of("동대문", "청량리", "회기", "전농", "답십리")),
    JUNGNANG(37.5954, 127.0930, List.of("중랑", "상봉", "망우", "면목")),
    SEONGBUK(37.5894, 127.0167, List.of("성북", "길음", "정릉", "돈암")),
    GANGBUK(37.6469, 127.0145, List.of("강북", "수유", "미아", "번동")),
    DOBONG(37.6688, 127.0471, List.of("도봉", "창동", "도봉동")),
    NOWON(37.6544, 127.0568, List.of("노원", "상계", "하계", "중계", "월계")),
    EUNPYEONG(37.6027, 126.9280, List.of("은평", "불광", "녹번", "응암")),
    SEODAEMUN(37.5791, 126.9368, List.of("서대문", "신촌", "홍은", "북가좌")),
    MAPO(37.5638, 126.9084, List.of("마포", "홍대", "서교", "합정", "상수", "망원")),
    YANGCHEON(37.5164, 126.8665, List.of("양천", "목동", "신정", "신월")),
    GANGSEO(37.5602, 126.8240, List.of("강서", "마곡", "가양", "발산", "방화", "염창")),
    GURO(37.4954, 126.8874, List.of("구로", "구로동", "신도림", "가산", "항동")),
    GEUMCHEON(37.4569, 126.8957, List.of("금천", "독산", "가산", "시흥")),
    YEONGDEUNGPO(37.5264, 126.8962, List.of("영등포", "여의도", "영등포동", "문래")),
    DONGJAK(37.5124, 126.9392, List.of("동작", "흑석", "노량진", "상도", "사당")),
    GWANAK(37.4784, 126.9516, List.of("관악", "봉천", "신림")),
    SEOCHO(37.4836, 127.0325, List.of("서초", "반포", "서초동", "잠원", "내곡")),
    GANGNAM(37.5172, 127.0473, List.of("강남", "삼성동", "역삼", "논현", "청담")),
    SONGPA(37.5145, 127.1056, List.of("송파", "잠실", "가락", "문정", "방이")),
    GANGDONG(37.5499, 127.1447, List.of("강동", "천호", "둔촌", "고덕"));

    public final double lat;
    public final double lng;
    public final List<String> keywords;


    // 사용자가 입력한 "강서", "강서구", "홍대", "성수", "마포", "여의도" 같은 단어 매칭
    public static SeoulRegion fromUserInput(String text) {
        if (text == null) return null;

        String cleaned = text.replace("구", "").trim();

        for (SeoulRegion region : values()) {
            for (String kw : region.keywords) {
                if (cleaned.contains(kw)) {
                    return region;
                }
            }
        }

        return null; // 못 찾으면 서울 전체
    }

}