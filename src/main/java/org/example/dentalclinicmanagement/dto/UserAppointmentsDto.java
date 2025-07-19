package org.example.dentalclinicmanagement.dto;

import lombok.Data;
import org.example.dentalclinicmanagement.model.Appointment;

import java.util.List;

@Data
public class UserAppointmentsDto {
    private List<Appointment> pastAppointments;
    private List<Appointment> todaysAppointments;
    private List<Appointment> thisWeekAppointments;
    private List<Appointment> futureAppointments;
}
