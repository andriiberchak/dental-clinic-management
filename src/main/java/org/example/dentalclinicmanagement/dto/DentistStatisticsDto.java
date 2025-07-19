package org.example.dentalclinicmanagement.dto;

import lombok.Data;

import java.util.List;

@Data
public class DentistStatisticsDto {
    private List<DailyCountDto> dailyCounts;
    private List<HourlyCountDto> hourlyCounts;
    private int totalCompletedAppointments;
    private double averageDurationMinutes;
}