package org.example.dentalclinicmanagement.dto;

import lombok.Data;

import java.util.List;

@Data
public class UserAppointmentsDto {
    private List<AppointmentDto> pastAppointments;
    private List<AppointmentDto> todaysAppointments;
    private List<AppointmentDto> thisWeekAppointments;
    private List<AppointmentDto> futureAppointments;
}
