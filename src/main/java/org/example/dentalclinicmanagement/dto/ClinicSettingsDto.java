package org.example.dentalclinicmanagement.dto;

public record ClinicSettingsDto(
        Integer booking24hLimit,
        Integer dailyBookingLimit,
        Integer dailyChangeLimit,
        Integer hourlyOverlapLimit,
        Integer modificationWindowHours
) {
}