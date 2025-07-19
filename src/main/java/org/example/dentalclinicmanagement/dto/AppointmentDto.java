package org.example.dentalclinicmanagement.dto;

import lombok.Builder;
import lombok.Data;
import org.example.dentalclinicmanagement.model.AppointmentStatus;

import java.time.LocalDateTime;

@Data
@Builder
public class AppointmentDto {
    private Long id;
    private Long dentistId;
    private String dentistName;
    private Long clientId;
    private String clientName;
    private LocalDateTime appointmentTime;
    private AppointmentStatus status;
    private Integer durationMinutes;
    private String comment;
    private LocalDateTime createdAt;
}