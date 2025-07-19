package org.example.dentalclinicmanagement.dto;

import lombok.Data;
import org.example.dentalclinicmanagement.model.AppointmentStatus;

import java.time.LocalDateTime;

@Data
public class TimeSlotDto {
    private Long appointmentId;
    private LocalDateTime slotTime;
    private AppointmentStatus status;
    private Integer durationMinutes;
    private Long clientId;
    private String clientName;
    private String firstName;
    private String lastName;
    private String comment;
}
