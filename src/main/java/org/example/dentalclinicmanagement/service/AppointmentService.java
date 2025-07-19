package org.example.dentalclinicmanagement.service;

import org.example.dentalclinicmanagement.dto.*;
import org.example.dentalclinicmanagement.dto.request.BookSlotRequest;
import org.example.dentalclinicmanagement.dto.request.CreateSlotRequest;
import org.example.dentalclinicmanagement.dto.request.UpdateAppointmentRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface AppointmentService {
    AppointmentDto createAppointmentSlot(Long dentistId, CreateSlotRequest request);
    AppointmentDto bookSlot(BookSlotRequest request, String userEmail);
    void cancelAppointment(Long appointmentId, String userEmail);
    AppointmentDto updateAppointment(Long appointmentId, UpdateAppointmentRequest request, String userEmail);
    void updateAppointmentComment(Long appointmentId, String comment, String userEmail);
    List<TimeSlotDto> getWeeklyCalendar(Long dentistId, LocalDate weekStart);
    List<TimeSlotDto> getPublicWeeklyCalendar(Long dentistId, LocalDate weekStart, String userEmail);
    UserAppointmentsDto getUserAppointmentsByTimeCategories(String userEmail);
    List<AppointmentDto> getPatientAppointmentHistory(Long patientId, Pageable pageable);
    long getPatientAppointmentCount(Long patientId);
    DentistStatisticsDto getDentistStatistics(Long dentistId, String period, LocalDate frameStart);
    List<NextFreeSlotDto> getNextFreeSlots(int requiredMinutes);
}