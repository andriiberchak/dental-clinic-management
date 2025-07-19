package org.example.dentalclinicmanagement.dto;

public record ClinicSettingsDto(
        Integer dailyBookingLimit,
        Integer booking24hLimit,
        Integer hourlyOverlapLimit,
        Integer modificationWindowHours,
        Integer dailyChangeLimit
) {
}