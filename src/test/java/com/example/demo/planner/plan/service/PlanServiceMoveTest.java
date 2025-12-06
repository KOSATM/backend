package com.example.demo.planner.plan.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.example.demo.planner.plan.dao.PlanDao;
import com.example.demo.planner.plan.dao.PlanDayDao;
import com.example.demo.planner.plan.dao.PlanPlaceDao;
import com.example.demo.planner.plan.dao.PlanSnapshotDao;
import com.example.demo.planner.plan.dto.entity.Plan;
import com.example.demo.planner.plan.dto.entity.PlanDay;
import com.example.demo.planner.plan.service.create.PlanService;

public class PlanServiceMoveTest {

    @Mock
    private PlanDao planDao;

    @Mock
    private PlanDayDao planDayDao;

    @Mock
    private PlanPlaceDao planPlaceDao;

    @Mock
    private PlanSnapshotDao planSnapshotDao;

    @InjectMocks
    private PlanService planService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testMoveDay_exceedsMax_throws() {
        // setup: days indices 1..5
        PlanDay moving = PlanDay.builder().id(100L).planId(1L).dayIndex(5).title("Day5").build();
        when(planDayDao.selectPlanDayById(100L)).thenReturn(moving);
        Plan plan = Plan.builder().id(1L).userId(10L).startDate(LocalDate.of(2025,12,10)).endDate(LocalDate.of(2025,12,14)).build();
        when(planDao.selectPlanById(1L)).thenReturn(plan);
        when(planDayDao.selectPlanDaysByPlanId(1L)).thenReturn(Arrays.asList(
            PlanDay.builder().id(90L).planId(1L).dayIndex(1).title("D1").build(),
            PlanDay.builder().id(91L).planId(1L).dayIndex(2).title("D2").build(),
            PlanDay.builder().id(92L).planId(1L).dayIndex(3).title("D3").build(),
            PlanDay.builder().id(93L).planId(1L).dayIndex(4).title("D4").build(),
            PlanDay.builder().id(100L).planId(1L).dayIndex(5).title("D5").build()
        ));

        // attempt to move to 7 (max is 5) without confirm -> should throw (requires confirmation)
        assertThrows(IllegalArgumentException.class, () -> planService.moveDay(100L, 7, null));
    }

    @Test
    public void testMoveDay_toMaxPlusOne_succeeds() {
        PlanDay moving = PlanDay.builder().id(200L).planId(2L).dayIndex(4).title("Day4").build();
        when(planDayDao.selectPlanDayById(200L)).thenReturn(moving);
        Plan plan = Plan.builder().id(2L).userId(11L).startDate(LocalDate.of(2025,12,10)).endDate(LocalDate.of(2025,12,13)).build();
        when(planDao.selectPlanById(2L)).thenReturn(plan);
        when(planDayDao.selectPlanDaysByPlanId(2L)).thenReturn(Arrays.asList(
            PlanDay.builder().id(190L).planId(2L).dayIndex(1).title("D1").build(),
            PlanDay.builder().id(191L).planId(2L).dayIndex(2).title("D2").build(),
            PlanDay.builder().id(192L).planId(2L).dayIndex(3).title("D3").build(),
            PlanDay.builder().id(200L).planId(2L).dayIndex(4).title("D4").build()
        ));

        // move to 5 (max+1) with confirm=true should not throw
        assertDoesNotThrow(() -> planService.moveDay(200L, 5, true));
    }
}
