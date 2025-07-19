package org.example.dentalclinicmanagement.controller;

import lombok.AllArgsConstructor;
import org.example.dentalclinicmanagement.dto.TimeSlotDto;
import org.example.dentalclinicmanagement.dto.UserAppointmentsDto;
import org.example.dentalclinicmanagement.dto.UserDto;
import org.example.dentalclinicmanagement.model.Appointment;
import org.example.dentalclinicmanagement.model.AppointmentStatus;
import org.example.dentalclinicmanagement.model.User;
import org.example.dentalclinicmanagement.service.AppointmentService;
import org.example.dentalclinicmanagement.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/appointments")
@AllArgsConstructor
public class AppointmentController {

    private final AppointmentService appointmentService;
    private final UserService userService;

    @PostMapping("/{appointmentId}/cancel")
    public ResponseEntity<?> cancelAppointment(@PathVariable Long appointmentId) {
        Appointment appt = appointmentService.cancelAppointment(appointmentId);
        return ResponseEntity.ok(appt);
    }

    @GetMapping("/calendar/{dentistId}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_MANAGER','ROLE_DENTIST')")
    public ResponseEntity<?> getWeeklyCalendar(@PathVariable Long dentistId,
                                               @RequestParam String weekStart) {
        List<TimeSlotDto> slots = appointmentService
                .getWeeklyCalendar(dentistId, LocalDate.parse(weekStart));
        return ResponseEntity.ok(slots);
    }

    @PostMapping("/book-slot")
    public ResponseEntity<?> bookSlot(@RequestParam Long dentistId,
                                      @RequestParam String slotTime,
                                      @RequestParam Long clientId,
                                      @RequestParam Integer durationMinutes,
                                      @RequestParam(required = false) String comment) {
        Appointment appt = appointmentService.bookSlot(
                dentistId,
                LocalDateTime.parse(slotTime),
                clientId,
                durationMinutes,
                comment
        );
        return ResponseEntity.ok(appt);
    }

    @PostMapping("/{dentistId}/create-slot")
    public ResponseEntity<?> createAppointmentSlot(@PathVariable Long dentistId,
                                                   @RequestParam String appointmentTime,
                                                   @RequestParam(required = false) String status,
                                                   @RequestParam Integer durationMinutes) {
        Appointment appt = appointmentService.createAppointmentSlot(
                dentistId,
                LocalDateTime.parse(appointmentTime),
                status != null
                        ? AppointmentStatus.valueOf(status.toUpperCase())
                        : AppointmentStatus.AVAILABLE,
                durationMinutes
        );
        return ResponseEntity.ok(appt);
    }

    @PostMapping("/{appointmentId}/update")
    public ResponseEntity<?> updateAppointment(@PathVariable Long appointmentId,
                                               @RequestParam String newTime) {
        Appointment appt = appointmentService.updateAppointment(
                appointmentId,
                LocalDateTime.parse(newTime)
        );
        return ResponseEntity.ok(appt);
    }

    @GetMapping("/my")
    public ResponseEntity<?> getUserAppointments(@AuthenticationPrincipal UserDetails userDetails) {
        UserDto user = userService.getUserByEmail(userDetails.getUsername());
        UserAppointmentsDto dto = appointmentService.getUserAppointmentsByTimeCategories(user);
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/public/calendar/{dentistId}")
    public ResponseEntity<?> getPublicWeeklyCalendar(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long dentistId,
            @RequestParam String weekStart
    ) {
        List<TimeSlotDto> slots = appointmentService
                .getWeeklyCalendar(dentistId, LocalDate.parse(weekStart));
        UserDto user = userService.getUserByEmail(userDetails.getUsername());
//        System.out.println(userDetails);
//        System.out.println("User ID: " + user.getUserId());
        slots.forEach(System.out::println);
        slots.forEach(slot -> {
            if (slot.getStatus() == AppointmentStatus.BOOKED
                    && !slot.getClientId().equals(user.getId())) {
                slot.setStatus(AppointmentStatus.BLOCKED);
                slot.setClientId(null);
                slot.setClientName(null);
                slot.setFirstName(null);
                slot.setLastName(null);
                slot.setComment(null);
            }
        });
        return ResponseEntity.ok(slots);
    }

    @PutMapping("/{appointmentId}/comment")
    public ResponseEntity<?> updateAppointmentComment(@PathVariable Long appointmentId,
                                                      @RequestParam(required = false) String comment) {
        Appointment updated = appointmentService.updateAppointmentComment(appointmentId, comment);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/patient-history/{patientId}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_MANAGER','ROLE_DENTIST')")
    public ResponseEntity<?> getPatientHistory(@PathVariable Long patientId,
                                               @RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "20") int size) {
        List<Appointment> history = appointmentService.getPatientAppointmentHistory(patientId, page, size);
        return ResponseEntity.ok(history);
    }

    @GetMapping("/patient-history/{patientId}/count")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_MANAGER','ROLE_DENTIST')")
    public ResponseEntity<?> getPatientHistoryCount(@PathVariable Long patientId) {
        long count = appointmentService.getPatientAppointmentCount(patientId);
        return ResponseEntity.ok(count);
    }
}
