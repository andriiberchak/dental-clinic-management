package org.example.dentalclinicmanagement.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class NextFreeSlotDto {
    private Long dentistId;
    private String dentistName;
    private LocalDateTime slotTime;
    private Integer durationMinutes;
}
