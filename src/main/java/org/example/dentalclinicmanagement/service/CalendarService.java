package org.example.dentalclinicmanagement.service;

import org.example.dentalclinicmanagement.model.Appointment;

public interface CalendarService {
    void pushAppointment(Appointment appointment);

    void deleteAppointment(Appointment appt);
}
