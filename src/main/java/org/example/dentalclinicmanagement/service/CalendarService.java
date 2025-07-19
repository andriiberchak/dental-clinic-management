package org.example.dentalclinicmanagement.service;

import org.example.dentalclinicmanagement.dto.CalendarEventDto;
import org.example.dentalclinicmanagement.model.Appointment;
import org.example.dentalclinicmanagement.model.User;

public interface CalendarService {
    CalendarEventDto syncAppointment(Appointment appointment);
    boolean removeAppointment(Appointment appointment);
    void storeCredentials(User user, String accessToken, String refreshToken, Long expiresAt);
}