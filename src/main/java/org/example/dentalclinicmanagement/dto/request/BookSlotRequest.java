package org.example.dentalclinicmanagement.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BookSlotRequest {
    
    @NotNull(message = "Dentist ID is required")
    private Long dentistId;
    
    @NotNull(message = "Slot time is required")
    @Future(message = "Appointment time must be in the future")
    private LocalDateTime slotTime;
    
    @NotNull(message = "Client ID is required")
    private Long clientId;
    
    @NotNull(message = "Duration is required")
    @Min(value = 15, message = "Duration must be at least 15 minutes")
    private Integer durationMinutes;
    
    @Size(max = 1000, message = "Comment cannot exceed 1000 characters")
    private String comment;
}