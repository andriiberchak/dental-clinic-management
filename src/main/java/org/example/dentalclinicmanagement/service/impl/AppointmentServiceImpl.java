package org.example.dentalclinicmanagement.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.dentalclinicmanagement.dto.*;
import org.example.dentalclinicmanagement.dto.request.BookSlotRequest;
import org.example.dentalclinicmanagement.dto.request.CreateSlotRequest;
import org.example.dentalclinicmanagement.dto.request.UpdateAppointmentRequest;
import org.example.dentalclinicmanagement.exception.AppointmentException;
import org.example.dentalclinicmanagement.exception.UserNotFoundException;
import org.example.dentalclinicmanagement.mapper.AppointmentMapper;
import org.example.dentalclinicmanagement.model.*;
import org.example.dentalclinicmanagement.repository.AppointmentRepository;
import org.example.dentalclinicmanagement.repository.UserRepository;
import org.example.dentalclinicmanagement.service.AppointmentService;
import org.example.dentalclinicmanagement.service.CalendarService;
import org.example.dentalclinicmanagement.service.ClinicSettingsService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class AppointmentServiceImpl implements AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final UserRepository userRepository;
    private final ClinicSettingsService settingsService;
    private final CalendarService calendarService;
    private final AppointmentMapper appointmentMapper;

    @Value("${clinic.work.start-hour:9}")
    private int workStartHour;

    @Value("${clinic.work.end-hour:20}")
    private int workEndHour;

    @Override
    @Transactional
    public AppointmentDto createAppointmentSlot(Long dentistId, CreateSlotRequest request) {
        log.debug("Creating appointment slot: dentistId={}, time={}", dentistId, request.getAppointmentTime());

        User dentist = findDentistById(dentistId);

        Optional<Appointment> existing = appointmentRepository
                .findByDentistAndAppointmentTime(dentist, request.getAppointmentTime());

        Appointment appointment = existing.orElseGet(Appointment::new);
        appointment.setDentist(dentist);
        appointment.setAppointmentTime(request.getAppointmentTime());
        appointment.setStatus(request.getStatus());
        appointment.setDurationMinutes(request.getDurationMinutes());

        Appointment saved = appointmentRepository.save(appointment);
        log.info("Appointment slot created: id={}", saved.getId());

        return appointmentMapper.toDto(saved);
    }

    @Override
    @Transactional
    public AppointmentDto bookSlot(BookSlotRequest request, String userEmail) {
        log.debug("Booking slot: dentistId={}, clientId={}, time={}",
                request.getDentistId(), request.getClientId(), request.getSlotTime());

        User dentist = findDentistById(request.getDentistId());
        User client = findUserById(request.getClientId());
        
        validateBookingPermissions(client, userEmail);
        validateBookingConstraints(client, request.getSlotTime());

        Optional<Appointment> existingOpt = appointmentRepository
                .findByDentistAndAppointmentTime(dentist, request.getSlotTime());

        Appointment appointment = existingOpt.map(existing -> {
            if (existing.getStatus() != AppointmentStatus.AVAILABLE) {
                throw new AppointmentException("Time slot is not available");
            }
            return existing;
        }).orElseGet(Appointment::new);

        appointment.setDentist(dentist);
        appointment.setAppointmentTime(request.getSlotTime());
        appointment.setClient(client);
        appointment.setStatus(AppointmentStatus.BOOKED);
        appointment.setDurationMinutes(request.getDurationMinutes());
        appointment.setComment(request.getComment());

        Appointment saved = appointmentRepository.save(appointment);

        try {
            calendarService.syncAppointment(saved);
        } catch (Exception ex) {
            log.warn("Failed to sync appointment {} with calendar: {}", saved.getId(), ex.getMessage());
        }

        log.info("Slot booked: appointmentId={}, clientId={}", saved.getId(), request.getClientId());
        return appointmentMapper.toDto(saved);
    }

    @Override
    @Transactional
    public void cancelAppointment(Long appointmentId, String userEmail) {
        log.debug("Cancelling appointment: {}", appointmentId);

        Appointment appointment = findAppointmentById(appointmentId);

        if (appointment.getStatus() == AppointmentStatus.AVAILABLE) {
            throw new AppointmentException("Appointment is not booked");
        }

        validateCancellationPermissions(appointment, userEmail);

        try {
            calendarService.removeAppointment(appointment);
        } catch (Exception ex) {
            log.warn("Failed to remove appointment {} from calendar: {}", appointmentId, ex.getMessage());
        }

        appointment.setClient(null);
        appointment.setStatus(AppointmentStatus.AVAILABLE);
        appointment.setReminderSent(false);
        appointment.setComment(null);

        appointmentRepository.save(appointment);
        log.info("Appointment cancelled: id={}", appointmentId);
    }

    @Override
    @Transactional
    public AppointmentDto updateAppointment(Long appointmentId, UpdateAppointmentRequest request, String userEmail) {
        log.debug("Updating appointment: id={}, newTime={}", appointmentId, request.getNewTime());

        Appointment appointment = findAppointmentById(appointmentId);

        validateUpdatePermissions(appointment, userEmail);
        checkTimeSlotConflicts(appointment, request.getNewTime());

        appointment.setAppointmentTime(request.getNewTime());
        Appointment saved = appointmentRepository.save(appointment);

        if (saved.getStatus() == AppointmentStatus.BOOKED && saved.getClient() != null) {
            try {
                calendarService.syncAppointment(saved);
            } catch (Exception ex) {
                log.warn("Failed to sync updated appointment {} with calendar: {}", appointmentId, ex.getMessage());
            }
        }

        log.info("Appointment updated: id={}", appointmentId);
        return appointmentMapper.toDto(saved);
    }

    @Override
    @Transactional
    public void updateAppointmentComment(Long appointmentId, String comment, String userEmail) {
        log.debug("Updating appointment comment: id={}", appointmentId);

        Appointment appointment = findAppointmentById(appointmentId);
        validateCommentUpdatePermissions(appointment, userEmail);
        
        appointment.setComment(comment);
        Appointment saved = appointmentRepository.save(appointment);
        
        if (saved.getStatus() == AppointmentStatus.BOOKED && saved.getClient() != null) {
            try {
                calendarService.syncAppointment(saved);
            } catch (Exception ex) {
                log.warn("Failed to sync comment update for appointment {} with calendar: {}", appointmentId, ex.getMessage());
            }
        }
        
        log.info("Appointment comment updated: id={}", appointmentId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TimeSlotDto> getWeeklyCalendar(Long dentistId, LocalDate weekStart) {
        log.debug("Getting weekly calendar: dentistId={}, weekStart={}", dentistId, weekStart);

        User dentist = findDentistById(dentistId);

        LocalDateTime periodStart = weekStart.atTime(6, 0);
        LocalDateTime periodEnd = weekStart.plusDays(6).atTime(22, 0);

        List<Appointment> appointments = appointmentRepository
                .findByDentistAndAppointmentTimeBetween(dentist, periodStart, periodEnd);

        return appointments.stream()
                .filter(appt -> appt.getStatus() != AppointmentStatus.AVAILABLE)
                .map(appointmentMapper::toTimeSlotDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TimeSlotDto> getPublicWeeklyCalendar(Long dentistId, LocalDate weekStart, String userEmail) {
        log.debug("Public calendar request: dentistId={}, weekStart={}, user={}",
                dentistId, weekStart, userEmail);

        User currentUser = findUserByEmail(userEmail);
        List<TimeSlotDto> slots = getWeeklyCalendar(dentistId, weekStart);

        slots.forEach(slot -> {
            if (slot.getStatus() == AppointmentStatus.BOOKED
                    && !currentUser.getId().equals(slot.getClientId())) {
                slot.setStatus(AppointmentStatus.BLOCKED);
                slot.setClientId(null);
                slot.setClientName(null);
                slot.setFirstName(null);
                slot.setLastName(null);
                slot.setComment(null);
            }
        });

        return slots;
    }

    @Override
    @Transactional(readOnly = true)
    public UserAppointmentsDto getUserAppointmentsByTimeCategories(String userEmail) {
        log.debug("Getting user appointments: user={}", userEmail);

        User user = findUserByEmail(userEmail);
        List<Appointment> allAppointments = appointmentRepository.findByClient(user);

        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime tomorrowStart = today.plusDays(1).atStartOfDay();
        LocalDate sunday = today.with(DayOfWeek.SUNDAY);
        LocalDateTime weekBoundary = sunday.atTime(23, 59, 59);

        Map<String, List<AppointmentDto>> categorized = allAppointments.stream()
                .collect(Collectors.groupingBy(
                        appt -> categorizeAppointment(appt, todayStart, tomorrowStart, weekBoundary),
                        Collectors.mapping(appointmentMapper::toDto, Collectors.toList())
                ));

        UserAppointmentsDto dto = new UserAppointmentsDto();
        dto.setPastAppointments(categorized.getOrDefault("past", Collections.emptyList()));
        dto.setTodaysAppointments(categorized.getOrDefault("today", Collections.emptyList()));
        dto.setThisWeekAppointments(categorized.getOrDefault("thisWeek", Collections.emptyList()));
        dto.setFutureAppointments(categorized.getOrDefault("future", Collections.emptyList()));

        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AppointmentDto> getPatientAppointmentHistory(Long patientId, Pageable pageable) {
        log.debug("Getting patient history: patientId={}, pageable={}", patientId, pageable);

        User patient = findUserById(patientId);

        List<Appointment> allAppointments = appointmentRepository.findByClient(patient)
                .stream()
                .filter(appt -> appt.getAppointmentTime().isBefore(LocalDateTime.now()))
                .sorted((a, b) -> b.getAppointmentTime().compareTo(a.getAppointmentTime()))
                .toList();

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), allAppointments.size());

        if (start >= allAppointments.size()) {
            return Collections.emptyList();
        }

        return allAppointments.subList(start, end)
                .stream()
                .map(appointmentMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public long getPatientAppointmentCount(Long patientId) {
        User patient = findUserById(patientId);
        return appointmentRepository.findByClient(patient)
                .stream()
                .filter(appt -> appt.getAppointmentTime().isBefore(LocalDateTime.now()))
                .count();
    }

    @Override
    @Transactional(readOnly = true)
    public DentistStatisticsDto getDentistStatistics(Long dentistId, String period, LocalDate frameStart) {
        log.debug("Getting dentist statistics: dentistId={}, period={}", dentistId, period);

        User dentist = findDentistById(dentistId);

        LocalDate[] dateRange = calculateDateRange(period, frameStart);
        LocalDate start = dateRange[0];
        LocalDate end = dateRange[1];

        LocalDateTime from = start.atStartOfDay();
        LocalDateTime to = end.atTime(23, 59, 59);

        List<Appointment> appointments = appointmentRepository
                .findByDentistAndAppointmentTimeBetween(dentist, from, to)
                .stream()
                .filter(a -> a.getStatus() == AppointmentStatus.BOOKED)
                .collect(Collectors.toList());

        return buildStatisticsDto(appointments, start, end);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NextFreeSlotDto> getNextFreeSlots(int requiredMinutes) {
        log.debug("Getting next free slots: requiredMinutes={}", requiredMinutes);

        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();
        LocalDate endDay = today.plusDays(7);

        List<User> dentists = userRepository.findByRole(Role.DENTIST);
        return dentists.stream()
                .map(dentist -> findNextFreeSlotForDentist(dentist, now, endDay, requiredMinutes))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    private User findDentistById(Long dentistId) {
        User dentist = findUserById(dentistId);
        if (!dentist.getRole().equals(Role.DENTIST)) {
            throw new AppointmentException("User is not a dentist");
        }
        return dentist;
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + email));
    }

    private Appointment findAppointmentById(Long appointmentId) {
        return appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new AppointmentException("Appointment not found with id: " + appointmentId));
    }

    private boolean isPrivileged() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;

        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> Set.of("ROLE_ADMIN", "ROLE_MANAGER", "ROLE_DENTIST").contains(role));
    }

    private void validateBookingPermissions(User client, String userEmail) {
        if (!isPrivileged() && !client.getEmail().equals(userEmail)) {
            throw new AppointmentException("You can only book appointments for yourself");
        }
    }

    private void validateCancellationPermissions(Appointment appointment, String userEmail) {
        if (!isPrivileged()) {
            if (appointment.getClient() == null || !appointment.getClient().getEmail().equals(userEmail)) {
                throw new AppointmentException("You can only cancel your own appointments");
            }

            ClinicSettingsDto settings = settingsService.getSettings();
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime cutoff = appointment.getAppointmentTime()
                    .minusHours(settings.modificationWindowHours());

            if (now.isAfter(cutoff)) {
                throw new AppointmentException(
                        "Cancellation allowed only " + settings.modificationWindowHours() +
                                " hours before appointment");
            }

            trackChange(appointment);
        }
    }

    private void validateUpdatePermissions(Appointment appointment, String userEmail) {
        if (!isPrivileged()) {
            if (appointment.getClient() == null || !appointment.getClient().getEmail().equals(userEmail)) {
                throw new AppointmentException("You can only update your own appointments");
            }

            ClinicSettingsDto settings = settingsService.getSettings();
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime cutoff = appointment.getAppointmentTime()
                    .minusHours(settings.modificationWindowHours());

            if (now.isAfter(cutoff)) {
                throw new AppointmentException(
                        "Update allowed only " + settings.modificationWindowHours() +
                                " hours before appointment");
            }

            trackChange(appointment);
        }
    }

    private void validateCommentUpdatePermissions(Appointment appointment, String userEmail) {
        if (!isPrivileged()) {
            if (appointment.getClient() == null || !appointment.getClient().getEmail().equals(userEmail)) {
                throw new AppointmentException("You can only update comments for your own appointments");
            }
        }
    }

    private void validateBookingConstraints(User client, LocalDateTime appointmentTime) {
        ClinicSettingsDto settings = settingsService.getSettings();

        LocalDate date = appointmentTime.toLocalDate();
        LocalDateTime dayStart = date.atStartOfDay();
        LocalDateTime dayEnd = date.atTime(23, 59, 59);

        long dailyCount = appointmentRepository
                .countByClientAndStatusAndAppointmentTimeBetween(
                        client, AppointmentStatus.BOOKED, dayStart, dayEnd);

        if (dailyCount >= settings.dailyBookingLimit()) {
            throw new AppointmentException("Daily booking limit exceeded");
        }

        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime todayEnd = today.plusDays(1).atStartOfDay().minusNanos(1);

        long createdToday = appointmentRepository
                .countByClientAndCreatedAtBetween(client, todayStart, todayEnd);

        if (createdToday >= settings.booking24hLimit()) {
            throw new AppointmentException("24h booking limit exceeded");
        }

        LocalDateTime hourStart = appointmentTime.truncatedTo(ChronoUnit.HOURS);
        LocalDateTime hourEnd = hourStart.plusHours(1).minusNanos(1);

        long overlap = appointmentRepository
                .countByClientAndStatusAndAppointmentTimeBetween(
                        client, AppointmentStatus.BOOKED, hourStart, hourEnd);

        if (overlap >= settings.hourlyOverlapLimit()) {
            throw new AppointmentException("Hourly overlap limit exceeded");
        }
    }

    private void checkTimeSlotConflicts(Appointment appointment, LocalDateTime newTime) {
        appointmentRepository.findByDentistAndAppointmentTime(appointment.getDentist(), newTime)
                .ifPresent(conflict -> {
                    if (!conflict.getId().equals(appointment.getId())) {
                        if (conflict.getStatus() == AppointmentStatus.BLOCKED) {
                            appointmentRepository.delete(conflict);
                        } else {
                            throw new AppointmentException("Time slot already booked");
                        }
                    }
                });
    }

    private void trackChange(Appointment appointment) {
        ClinicSettingsDto settings = settingsService.getSettings();
        LocalDate today = LocalDate.now();

        if (!today.equals(appointment.getLastChangeDate())) {
            appointment.setDailyChangeCount(0);
        }

        if (appointment.getDailyChangeCount() >= settings.dailyChangeLimit()) {
            throw new AppointmentException(
                    "Daily change limit of " + settings.dailyChangeLimit() + " exceeded");
        }

        appointment.setDailyChangeCount(appointment.getDailyChangeCount() + 1);
        appointment.setLastChangeDate(today);
    }

    private String categorizeAppointment(Appointment appt,
                                         LocalDateTime todayStart,
                                         LocalDateTime tomorrowStart,
                                         LocalDateTime weekBoundary) {
        LocalDateTime apptTime = appt.getAppointmentTime();

        if (apptTime.isBefore(todayStart)) return "past";
        if (apptTime.isBefore(tomorrowStart)) return "today";
        if (!apptTime.isAfter(weekBoundary)) return "thisWeek";
        return "future";
    }

    private LocalDate[] calculateDateRange(String period, LocalDate frameStart) {
        LocalDate today = LocalDate.now();
        LocalDate start;

        LocalDate base = Objects.requireNonNullElse(frameStart, today);

        start = switch (period.toLowerCase()) {
            case "week" -> base.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            case "month" -> base.withDayOfMonth(1);
            case "year" -> base.withDayOfYear(1);
            default -> base;
        };

        LocalDate end = switch (period.toLowerCase()) {
            case "week" -> start.plusDays(6);
            case "month" -> start.plusMonths(1).minusDays(1);
            case "year" -> start.plusYears(1).minusDays(1);
            default -> today;
        };

        return new LocalDate[]{start, end};
    }

    private DentistStatisticsDto buildStatisticsDto(List<Appointment> appointments,
                                                    LocalDate start,
                                                    LocalDate end) {
        Map<LocalDate, Long> dailyCounts = appointments.stream()
                .collect(Collectors.groupingBy(
                        a -> a.getAppointmentTime().toLocalDate(),
                        Collectors.counting()));

        Map<Integer, Long> hourlyCounts = appointments.stream()
                .collect(Collectors.groupingBy(
                        a -> a.getAppointmentTime().getHour(),
                        Collectors.counting()));

        long days = ChronoUnit.DAYS.between(start, end) + 1;
        List<DailyCountDto> dailyCountsList = Stream.iterate(start, d -> d.plusDays(1))
                .limit(days)
                .map(d -> new DailyCountDto(d, dailyCounts.getOrDefault(d, 0L)))
                .collect(Collectors.toList());

        List<HourlyCountDto> hourlyCountsList = IntStream.range(0, 24)
                .mapToObj(h -> new HourlyCountDto(h, hourlyCounts.getOrDefault(h, 0L)))
                .collect(Collectors.toList());

        double avgDuration = appointments.stream()
                .mapToDouble(Appointment::getDurationMinutes)
                .average()
                .orElse(0.0);

        DentistStatisticsDto dto = new DentistStatisticsDto();
        dto.setDailyCounts(dailyCountsList);
        dto.setHourlyCounts(hourlyCountsList);
        dto.setTotalCompletedAppointments(appointments.size());
        dto.setAverageDurationMinutes(avgDuration);

        return dto;
    }

    private Optional<NextFreeSlotDto> findNextFreeSlotForDentist(User dentist,
                                                                 LocalDateTime now,
                                                                 LocalDate endDay,
                                                                 int requiredMinutes) {
        LocalDateTime windowStart = now.toLocalDate().atStartOfDay();
        LocalDateTime windowEnd = endDay.atTime(23, 59, 59);

        Set<LocalDateTime> occupied = appointmentRepository
                .findByDentistAndAppointmentTimeBetween(dentist, windowStart, windowEnd)
                .stream()
                .filter(a -> a.getStatus() != AppointmentStatus.AVAILABLE)
                .map(Appointment::getAppointmentTime)
                .collect(Collectors.toSet());

        LocalDate scanDate = now.toLocalDate();
        while (!scanDate.isAfter(endDay)) {
            for (int hour = workStartHour; hour <= workEndHour; hour++) {
                LocalDateTime slot = scanDate.atTime(hour, 0);
                if (slot.isBefore(now)) continue;

                if (!occupied.contains(slot)) {
                    return Optional.of(new NextFreeSlotDto(
                            dentist.getId(),
                            dentist.getFirstName() + " " + dentist.getLastName(),
                            slot,
                            requiredMinutes
                    ));
                }
            }
            scanDate = scanDate.plusDays(1);
        }

        return Optional.empty();
    }
}