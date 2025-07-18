package org.example.dentalclinicmanagement.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record ClinicSettingsDto(
        @Min(value = 0, message = "Modification window hours must be non-negative")
        @Max(value = 48, message = "Modification window hours cannot exceed 48")
        Integer modificationWindowHours,

        @Min(value = 0, message = "Daily change limit must be non-negative")
        @Max(value = 20, message = "Daily change limit cannot exceed 20")
        Integer dailyChangeLimit,

        @Min(value = 0, message = "Daily booking limit must be non-negative")
        @Max(value = 10, message = "Daily booking limit cannot exceed 10")
        Integer dailyBookingLimit,

        @Min(value = 0, message = "24h booking limit must be non-negative")
        @Max(value = 50, message = "24h booking limit cannot exceed 50")
        Integer booking24hLimit,

        @Min(value = 0, message = "Hourly overlap limit must be non-negative")
        @Max(value = 5, message = "Hourly overlap limit cannot exceed 5")
        Integer hourlyOverlapLimit
) {
}