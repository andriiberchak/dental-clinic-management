package org.example.dentalclinicmanagement.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.dentalclinicmanagement.dto.*;
import org.example.dentalclinicmanagement.dto.request.BookSlotRequest;
import org.example.dentalclinicmanagement.dto.request.CreateSlotRequest;
import org.example.dentalclinicmanagement.dto.request.UpdateAppointmentRequest;
import org.example.dentalclinicmanagement.dto.response.MessageResponse;
import org.example.dentalclinicmanagement.service.AppointmentService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
@Slf4j
@Validated
public class AppointmentController {

    private final AppointmentService appointmentService;

    @PostMapping("/{appointmentId}/cancel")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MessageResponse> cancelAppointment(
            @PathVariable @NotNull Long appointmentId,
            @AuthenticationPrincipal UserDetails userDetails) {
        log.info("Cancel appointment request: appointmentId={}, user={}", appointmentId, userDetails.getUsername());

        appointmentService.cancelAppointment(appointmentId, userDetails.getUsername());
        return ResponseEntity.ok(new MessageResponse("Appointment cancelled successfully"));
    }

    @GetMapping("/calendar/{dentistId}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','DENTIST')")
    public ResponseEntity<List<TimeSlotDto>> getWeeklyCalendar(
            @PathVariable @NotNull Long dentistId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart) {

        log.debug("Weekly calendar request: dentistId={}, weekStart={}", dentistId, weekStart);

        List<TimeSlotDto> slots = appointmentService.getWeeklyCalendar(dentistId, weekStart);
        return ResponseEntity.ok(slots);
    }

    @PostMapping("/book-slot")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AppointmentDto> bookSlot(
            @Valid @RequestBody BookSlotRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        log.info("Book slot request: {}, user={}", request, userDetails.getUsername());

        AppointmentDto appointment = appointmentService.bookSlot(request, userDetails.getUsername());
        return ResponseEntity.ok(appointment);
    }

    @PostMapping("/{dentistId}/create-slot")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','DENTIST')")
    public ResponseEntity<AppointmentDto> createAppointmentSlot(
            @PathVariable @NotNull Long dentistId,
            @Valid @RequestBody CreateSlotRequest request) {

        log.info("Create slot request: dentistId={}, request={}", dentistId, request);

        AppointmentDto appointment = appointmentService.createAppointmentSlot(dentistId, request);
        return ResponseEntity.ok(appointment);
    }

    @PutMapping("/{appointmentId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AppointmentDto> updateAppointment(
            @PathVariable @NotNull Long appointmentId,
            @Valid @RequestBody UpdateAppointmentRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("Update appointment request: appointmentId={}, newTime={}, user={}",
                appointmentId, request.getNewTime(), userDetails.getUsername());

        AppointmentDto appointment = appointmentService.updateAppointment(appointmentId, request, userDetails.getUsername());
        return ResponseEntity.ok(appointment);
    }

    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserAppointmentsDto> getUserAppointments(
            @AuthenticationPrincipal UserDetails userDetails) {

        log.debug("User appointments request: user={}", userDetails.getUsername());

        UserAppointmentsDto dto = appointmentService.getUserAppointmentsByTimeCategories(userDetails.getUsername());
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/public/calendar/{dentistId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<TimeSlotDto>> getPublicWeeklyCalendar(
            @PathVariable @NotNull Long dentistId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.debug("Public calendar request: dentistId={}, weekStart={}, user={}",
                dentistId, weekStart, userDetails.getUsername());

        List<TimeSlotDto> slots = appointmentService.getPublicWeeklyCalendar(dentistId, weekStart, userDetails.getUsername());
        return ResponseEntity.ok(slots);
    }

    @PatchMapping("/{appointmentId}/comment")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MessageResponse> updateAppointmentComment(
            @PathVariable @NotNull Long appointmentId,
            @RequestParam(required = false) String comment,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("Update comment request: appointmentId={}, user={}", appointmentId, userDetails.getUsername());

        appointmentService.updateAppointmentComment(appointmentId, comment, userDetails.getUsername());
        return ResponseEntity.ok(new MessageResponse("Comment updated successfully"));
    }

    @GetMapping("/patient-history/{patientId}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','DENTIST')")
    public ResponseEntity<List<AppointmentDto>> getPatientHistory(
            @PathVariable @NotNull Long patientId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) int size) {

        log.debug("Patient history request: patientId={}, page={}, size={}", patientId, page, size);

        Pageable pageable = PageRequest.of(page, size);
        List<AppointmentDto> history = appointmentService.getPatientAppointmentHistory(patientId, pageable);
        return ResponseEntity.ok(history);
    }

    @GetMapping("/patient-history/{patientId}/count")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','DENTIST')")
    public ResponseEntity<Long> getPatientHistoryCount(@PathVariable @NotNull Long patientId) {
        log.debug("Patient history count request: patientId={}", patientId);

        long count = appointmentService.getPatientAppointmentCount(patientId);
        return ResponseEntity.ok(count);
    }

    @GetMapping("/statistics/{dentistId}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','DENTIST')")
    public ResponseEntity<DentistStatisticsDto> getDentistStatistics(
            @PathVariable @NotNull Long dentistId,
            @RequestParam(defaultValue = "week") String period,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate frameStart) {

        log.debug("Statistics request: dentistId={}, period={}, frameStart={}",
                dentistId, period, frameStart);

        DentistStatisticsDto statistics = appointmentService.getDentistStatistics(dentistId, period, frameStart);
        return ResponseEntity.ok(statistics);
    }

    @GetMapping("/next-free-slots")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<NextFreeSlotDto>> getNextFreeSlots(
            @RequestParam(defaultValue = "30") @Min(1) int requiredMinutes) {

        log.debug("Next free slots request: requiredMinutes={}", requiredMinutes);

        List<NextFreeSlotDto> slots = appointmentService.getNextFreeSlots(requiredMinutes);
        return ResponseEntity.ok(slots);
    }
}