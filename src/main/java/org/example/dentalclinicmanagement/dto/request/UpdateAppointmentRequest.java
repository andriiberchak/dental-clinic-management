package org.example.dentalclinicmanagement.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UpdateAppointmentRequest {
    
    @NotNull(message = "New appointment time is required")
    @Future(message = "New appointment time must be in the future")
    private LocalDateTime newTime;
}