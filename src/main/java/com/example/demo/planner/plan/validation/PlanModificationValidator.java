package com.example.demo.planner.plan.validation;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Component;

import com.example.demo.planner.plan.dto.entity.Plan;
import com.example.demo.planner.plan.service.create.PlanService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Safety Layer for Plan Modifications
 * 
 * Purpose:
 * - Prevent LLM from making invalid modifications
 * - Validate all edit/delete operations before execution
 * - Ensure data integrity and consistency
 * 
 * 3-Layer Protection:
 * 1. Intent Level Validation (existence, range, ownership)
 * 2. Schema Level Protection (allowed fields only)
 * 3. Transactional Rollback (automatic on error)
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class PlanModificationValidator {

    private final PlanService planService;

    // ==================== EXISTENCE VALIDATION ====================

    /**
     * Validate that user has an active plan
     */
    public Plan validateUserHasActivePlan(Long userId) {
        Plan plan = planService.findActiveByUserId(userId);
        if (plan == null) {
            throw new PlanValidationException("No active travel plan found for user " + userId);
        }
        return plan;
    }

    /**
     * Validate that day exists in plan
     */
    public void validateDayExists(Long planId, int dayIndex) {
        try {
            planService.queryDay(planId, dayIndex);
        } catch (Exception e) {
            throw new PlanValidationException("Day " + dayIndex + " does not exist in plan " + planId);
        }
    }

    /**
     * Validate that multiple days exist
     */
    public void validateDaysExist(Long planId, int... dayIndices) {
        for (int dayIndex : dayIndices) {
            validateDayExists(planId, dayIndex);
        }
    }

    /**
     * Validate that place exists at specific location
     */
    public void validatePlaceExists(Long planId, int dayIndex, int placeIndex) {
        try {
            planService.queryPlace(planId, dayIndex, placeIndex);
        } catch (Exception e) {
            throw new PlanValidationException(
                "Place " + placeIndex + " on day " + dayIndex + " does not exist"
            );
        }
    }

    // ==================== RANGE VALIDATION ====================

    /**
     * Validate day index is within valid range
     */
    public void validateDayIndexRange(int dayIndex, int minDay, int maxDay) {
        if (dayIndex < minDay || dayIndex > maxDay) {
            throw new PlanValidationException(
                "Day index " + dayIndex + " is out of range [" + minDay + ", " + maxDay + "]"
            );
        }
    }

    /**
     * Validate place order is within valid range
     */
    public void validatePlaceOrderRange(int order, int minOrder, int maxOrder) {
        if (order < minOrder || order > maxOrder) {
            throw new PlanValidationException(
                "Place order " + order + " is out of range [" + minOrder + ", " + maxOrder + "]"
            );
        }
    }

    // ==================== DATE VALIDATION ====================

    /**
     * Validate that new date range is valid
     */
    public void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new PlanValidationException("Start date and end date cannot be null");
        }
        if (startDate.isAfter(endDate)) {
            throw new PlanValidationException("Start date cannot be after end date");
        }
        if (startDate.isBefore(LocalDate.now())) {
            log.warn("Start date {} is in the past", startDate);
        }
    }

    /**
     * Validate that date range change is reasonable
     */
    public void validateDateRangeChange(Plan currentPlan, LocalDate newStartDate, LocalDate newEndDate) {
        validateDateRange(newStartDate, newEndDate);
        
        long currentDays = currentPlan.getEndDate().toEpochDay() - currentPlan.getStartDate().toEpochDay() + 1;
        long newDays = newEndDate.toEpochDay() - newStartDate.toEpochDay() + 1;
        
        if (newDays != currentDays) {
            log.warn("Trip duration is changing from {} days to {} days", currentDays, newDays);
        }
    }

    // ==================== SWAP VALIDATION ====================

    /**
     * Validate day swap operation
     */
    public void validateDaySwap(Long planId, int dayA, int dayB) {
        if (dayA == dayB) {
            throw new PlanValidationException("Cannot swap a day with itself");
        }
        validateDaysExist(planId, dayA, dayB);
    }

    /**
     * Validate place swap within same day
     */
    public void validatePlaceSwapInner(Long planId, int dayIndex, int placeA, int placeB) {
        if (placeA == placeB) {
            throw new PlanValidationException("Cannot swap a place with itself");
        }
        validatePlaceExists(planId, dayIndex, placeA);
        validatePlaceExists(planId, dayIndex, placeB);
    }

    /**
     * Validate place swap between different days
     */
    public void validatePlaceSwapBetween(Long planId, int dayA, int placeA, int dayB, int placeB) {
        if (dayA == dayB && placeA == placeB) {
            throw new PlanValidationException("Cannot swap a place with itself");
        }
        validatePlaceExists(planId, dayA, placeA);
        validatePlaceExists(planId, dayB, placeB);
    }

    // ==================== DELETE VALIDATION ====================

    /**
     * Validate place deletion
     */
    public void validatePlaceDelete(Long planId, int dayIndex, int placeIndex) {
        validatePlaceExists(planId, dayIndex, placeIndex);
        
        // Check if this is the last place in the day (optional warning)
        try {
            var dayWithPlaces = planService.queryDay(planId, dayIndex);
            if (dayWithPlaces.getPlaces().size() == 1) {
                log.warn("Deleting the last place from day {}", dayIndex);
            }
        } catch (Exception e) {
            // Continue anyway
        }
    }

    /**
     * Validate day deletion
     */
    public void validateDayDelete(Long planId, int dayIndex) {
        validateDayExists(planId, dayIndex);
        
        // Log deletion for monitoring
        log.info("Deleting day {} from plan {}", dayIndex, planId);
    }

    // ==================== FIELD UPDATE VALIDATION ====================

    /**
     * Validate that only allowed fields are being updated
     * This prevents LLM from accidentally modifying critical fields
     */
    public void validateAllowedFieldUpdate(String fieldName) {
        List<String> allowedFields = List.of(
            "placeName", "address", "startTime", "endTime", 
            "duration", "cost", "lat", "lng", "category",
            "planDayId", "order", "title"
        );
        
        if (!allowedFields.contains(fieldName)) {
            throw new PlanValidationException(
                "Field '" + fieldName + "' is not allowed to be updated"
            );
        }
    }

    /**
     * Validate that forbidden fields are NOT being updated
     */
    public void validateForbiddenFieldNotUpdated(String fieldName) {
        List<String> forbiddenFields = List.of(
            "id", "userId", "createdAt", "planId"
        );
        
        if (forbiddenFields.contains(fieldName)) {
            throw new PlanValidationException(
                "Field '" + fieldName + "' is forbidden from being updated"
            );
        }
    }

    // ==================== CUSTOM EXCEPTION ====================

    public static class PlanValidationException extends RuntimeException {
        public PlanValidationException(String message) {
            super(message);
        }
    }
}
