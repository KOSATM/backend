package com.example.demo.travelgram.review.dto.request;

import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class HashtagUpdateRequest {
    private Long hashtagGroupId;
    private List<String> names; // ["여행", "맛집", "힐링"] 처럼 들어옴
}
