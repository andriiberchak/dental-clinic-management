package org.example.dentalclinicmanagement.dto;

import lombok.Data;
import org.example.dentalclinicmanagement.model.AppointmentStatus;

import java.time.LocalDateTime;

@Data
public class TimeSlotDto {
    private Long appointmentId;
    private LocalDateTime slotTime;       // час слоту
    private AppointmentStatus status;     // статус слоту (AVAILABLE, BOOKED, BLOCKED тощо)
    private Integer durationMinutes;

    // Дані про клієнта (пацієнта), який зробив запис (якщо запис заброньовано)
    private Long clientId;
    private String clientName;
    private String firstName;
    private String lastName;
    private String comment;
}
