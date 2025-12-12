package com.example.demo.planner.plan.service;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class RestPlaceService {

  @Autowired
  private JdbcTemplate jdbcTemplate;

  public Object searchRestPlace(Double curLat, Double curLng) {
    log.info("위도: {}, 경도: {}", curLat, curLng);

    String sql = """
        SELECT id, content_id, title, address, tel, first_image, first_image2, lat, lng,
               description, normalized_category,
                (6371 * acos(
                    cos(radians(?)) * cos(radians(lat)) *
                    cos(radians(lng) - radians(?)) +
                    sin(radians(?)) * sin(radians(lat))
                )) AS distance
        FROM travel_places
        WHERE normalized_category = 'CAFE' OR title LIKE '%공원'
        ORDER BY distance
        LIMIT 10
        """;

    List<Map<String, Object>> res = jdbcTemplate.queryForList(sql, curLat, curLng, curLat);
    log.info("res: {}", res.toString());
    
    // List<TravelPlaces> travelPlacesList = new ArrayList<>();
    // for (Map<String, Object> row in)
    return res;

  }

}