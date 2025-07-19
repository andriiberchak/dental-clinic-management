package org.example.dentalclinicmanagement.dto;

import lombok.Data;

import java.util.List;

@Data
public class DentistStatisticsDto {
    private List<DailyCountDto> dailyCounts;
    private List<HourlyCountDto> hourlyCounts;
    private long totalCompletedAppointments;

    /** +++ NEW: overall average duration (in minutes) for the selected period +++ */
    private double averageDurationMinutes;

    // getters / setters

    public List<DailyCountDto> getDailyCounts() { return dailyCounts; }
    public void setDailyCounts(List<DailyCountDto> dailyCounts) { this.dailyCounts = dailyCounts; }

    public List<HourlyCountDto> getHourlyCounts() { return hourlyCounts; }
    public void setHourlyCounts(List<HourlyCountDto> hourlyCounts) { this.hourlyCounts = hourlyCounts; }

    public long getTotalCompletedAppointments() { return totalCompletedAppointments; }
    public void setTotalCompletedAppointments(long totalCompletedAppointments) {
        this.totalCompletedAppointments = totalCompletedAppointments;
    }

    public double getAverageDurationMinutes() { return averageDurationMinutes; }
    public void setAverageDurationMinutes(double averageDurationMinutes) {
        this.averageDurationMinutes = averageDurationMinutes;
    }
}