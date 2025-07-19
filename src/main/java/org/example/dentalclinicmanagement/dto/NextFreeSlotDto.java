package org.example.dentalclinicmanagement.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NextFreeSlotDto {
    private Long dentistId;
    private String dentistName;
    private LocalDateTime slotTime;
    private Integer durationMinutes;
}