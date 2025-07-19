package org.example.dentalclinicmanagement.listener;

import jakarta.persistence.PostPersist;
import jakarta.persistence.PostUpdate;
import lombok.extern.slf4j.Slf4j;
import org.example.dentalclinicmanagement.model.Appointment;
import org.example.dentalclinicmanagement.model.AppointmentStatus;
import org.example.dentalclinicmanagement.service.CalendarService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class AppointmentEntityListener {

    private static CalendarService calendarService;

    @Autowired
    public void setCalendarService(CalendarService calendarService) {
        AppointmentEntityListener.calendarService = calendarService;
    }

    @PostPersist
    @PostUpdate
    public void afterSave(Appointment appointment) {
        if (calendarService == null) {
            log.warn("CalendarService not available in entity listener");
            return;
        }

        try {
            if (appointment.getStatus() == AppointmentStatus.BOOKED && appointment.getClient() != null) {
                log.debug("Syncing booked appointment to calendar: id={}", appointment.getId());
                calendarService.syncAppointment(appointment);
            } else if (appointment.getStatus() == AppointmentStatus.AVAILABLE) {
                log.debug("Removing appointment from calendar: id={}", appointment.getId());
                calendarService.removeAppointment(appointment);
            }
        } catch (Exception ex) {
            log.error("Error syncing appointment {} to calendar: {}", appointment.getId(), ex.getMessage(), ex);
        }
    }
}