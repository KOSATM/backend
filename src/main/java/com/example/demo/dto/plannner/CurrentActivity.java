package com.example.demo.dto.plannner;

import java.time.LocalDate;
import lombok.Data;

@Data
public class CurrentActivity {
    private Long id;
    private Long travelspotId;
    private Long actualCost;
    private String review;
    private LocalDate endedAt;
}
