package org.example.dentalclinicmanagement.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.example.dentalclinicmanagement.model.AppointmentStatus;

import java.time.LocalDateTime;

@Data
public class CreateSlotRequest {
    
    @NotNull(message = "Appointment time is required")
    @Future(message = "Appointment time must be in the future")
    private LocalDateTime appointmentTime;
    
    private AppointmentStatus status = AppointmentStatus.AVAILABLE;
    
    @NotNull(message = "Duration is required")
    @Min(value = 15, message = "Duration must be at least 15 minutes")
    private Integer durationMinutes;
}
