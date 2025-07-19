package org.example.dentalclinicmanagement.service;


import org.example.dentalclinicmanagement.dto.*;
import org.example.dentalclinicmanagement.model.Appointment;
import org.example.dentalclinicmanagement.model.AppointmentStatus;
import org.example.dentalclinicmanagement.model.User;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface AppointmentService {

    Appointment createAppointmentSlot(Long dentistId, LocalDateTime appointmentTime, AppointmentStatus status, Integer durationMinutes);


    Appointment cancelAppointment(Long appointmentId);

    List<TimeSlotDto> getWeeklyCalendar(Long dentistId, LocalDate weekStart);

    Appointment bookSlot(Long dentistId, LocalDateTime appointmentTime, Long clientId, Integer durationMinutes, String comment);

    Appointment updateAppointment(Long appointmentId, LocalDateTime newAppointmentTime);

    UserAppointmentsDto getUserAppointmentsByTimeCategories(UserDto user);

    Appointment updateAppointmentComment(Long appointmentId, String comment);

    DentistStatisticsDto getDentistStatistics(Long dentistId,
                                              String period,
                                              LocalDate frameStart);

    List<NextFreeSlotDto> getNextFreeSlotsForAllDentists(int requiredMinutes);

    List<NextFreeSlotDto> getNextFreeSlots(int requiredMinutes);

    List<Appointment> getPatientAppointmentHistory(Long patientId, int page, int size);

    long getPatientAppointmentCount(Long patientId);

}
