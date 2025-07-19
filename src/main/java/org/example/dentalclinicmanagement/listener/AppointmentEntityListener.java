package org.example.dentalclinicmanagement.listener;

import jakarta.persistence.PostPersist;
import jakarta.persistence.PostUpdate;
import org.example.dentalclinicmanagement.model.Appointment;
import org.example.dentalclinicmanagement.model.AppointmentStatus;
import org.example.dentalclinicmanagement.service.CalendarService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AppointmentEntityListener {

    private static CalendarService calendarService;

    @Autowired
    public void init(CalendarService svc) {
        AppointmentEntityListener.calendarService = svc;
    }

    @PostPersist @PostUpdate
    public void afterSave(Appointment appt) {
        if (appt.getStatus() == AppointmentStatus.BOOKED
                && appt.getClient() != null) {
            calendarService.pushAppointment(appt);
        }else if (appt.getStatus() == AppointmentStatus.AVAILABLE) {
            calendarService.deleteAppointment(appt);           // NEW
        }
    }
}
