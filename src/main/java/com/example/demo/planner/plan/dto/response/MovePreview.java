package com.example.demo.planner.plan.dto.response;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MovePreview {
  private boolean requiresExtension;
  private LocalDate newEndDate;
  private int currentMaxIndex;
  private int requestedToIndex;
}
