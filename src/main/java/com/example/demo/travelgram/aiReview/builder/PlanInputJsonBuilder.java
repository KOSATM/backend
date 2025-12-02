package com.example.demo.travelgram.aiReview.builder;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.example.demo.planner.plan.dao.CurrentActivityDao;
import com.example.demo.planner.plan.dto.entity.CurrentActivity;
import com.example.demo.planner.plan.dto.entity.Plan;
import com.example.demo.planner.plan.dto.entity.PlanDay;
import com.example.demo.planner.plan.dto.entity.PlanPlace;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Component
public class PlanInputJsonBuilder {

    private final ObjectMapper mapper = new ObjectMapper();
    private final CurrentActivityDao activityDao;

    public ObjectNode build(Plan plan,
                            List<PlanDay> planDays,
                            Map<Long, List<PlanPlace>> placesByDayId) {

        ObjectNode root = mapper.createObjectNode();

        // 기본 정보
        root.put("trip_title", plan.getTitle());
        root.put("start_date", plan.getStartDate().toString());
        root.put("end_date", plan.getEndDate().toString());

        // days 배열 생성
        ArrayNode daysArray = mapper.createArrayNode();

        for (PlanDay day : planDays) {

            ObjectNode dayNode = mapper.createObjectNode();
            dayNode.put("day", day.getDayIndex());

            // places 배열
            ArrayNode placeArray = mapper.createArrayNode();

            List<PlanPlace> places = placesByDayId.get(day.getId());

            if (places != null) {
                for (PlanPlace place : places) {
                    ObjectNode placeNode = mapper.createObjectNode();
                    placeNode.put("place_name", place.getTitle());

                    CurrentActivity activity = activityDao.selectByPlaceId(place.getId());
                    if (activity != null) {
                        placeNode.put("memo", activity.getMemo() == null ? "" : activity.getMemo());
                        placeNode.put("actual_cost", activity.getActualCost() == null ? 0 : activity.getActualCost());
                    } else {
                        placeNode.put("memo", "");
                        placeNode.put("actual_cost", 0);
                    }
                    placeArray.add(placeNode);
                }
            }

            dayNode.set("places", placeArray);
            daysArray.add(dayNode);
        }

        root.set("days", daysArray);

        return root;
    }
}
