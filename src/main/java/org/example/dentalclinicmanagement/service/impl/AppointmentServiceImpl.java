package org.example.dentalclinicmanagement.service.impl;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.example.dentalclinicmanagement.dto.*;
import org.example.dentalclinicmanagement.exception.UserNotFoundException;
import org.example.dentalclinicmanagement.model.*;
import org.example.dentalclinicmanagement.repository.AppointmentRepository;
import org.example.dentalclinicmanagement.repository.UserRepository;
import org.example.dentalclinicmanagement.service.AppointmentService;
import org.example.dentalclinicmanagement.service.ClinicSettingsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
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

import static java.time.DayOfWeek.*;

@Service
@RequiredArgsConstructor
public class AppointmentServiceImpl implements AppointmentService {

    private AppointmentRepository appointmentRepository;

    private UserRepository userRepository;

    private GoogleCalendarService calendarService;   // ←

    @Value("${clinic.work.start-hour:9}")
    private int workStartHour;

    @Value("${clinic.work.end-hour:20}")
    private int workEndHour;
    private ClinicSettingsService settingsService;


    private boolean isPrivileged() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        return auth.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .anyMatch(r -> r.equals(Role.ADMIN.name())
                        || r.equals(Role.MANAGER.name())
                        || r.equals(Role.DENTIST.name()));
    }

    @Override
    public Appointment createAppointmentSlot(Long dentistId, LocalDateTime appointmentTime, AppointmentStatus status, Integer durationMinutes) {
        User dentist = userRepository.findById(dentistId)
                .orElseThrow(() -> new RuntimeException("Dentist not found"));

        // Перевірка ролі стоматолога
        if (!dentist.getRole().equals(Role.DENTIST)) {
            throw new RuntimeException("Користувач не є стоматологом");
        }
        Optional<Appointment> appt = appointmentRepository.findByDentistAndAppointmentTime(dentist, appointmentTime);
        Appointment appointment = appt.orElseGet(Appointment::new);
        appointment.setDentist(dentist);
        appointment.setAppointmentTime(appointmentTime);
        appointment.setStatus(status);
        appointment.setDurationMinutes(durationMinutes);
        return appointmentRepository.save(appointment);
    }


    @Override
    @Transactional
    public Appointment cancelAppointment(Long appointmentId) {
        ClinicSettingsDto cfg = settingsService.getSettings();
        // наприклад:
        int modificationWindowHours = cfg.modificationWindowHours();
        int dailyChangeLimit = cfg.dailyChangeLimit();
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Запис не знайдено"));

        // 1) Перевіряємо, чи це не “AVAILABLE”
        if (appointment.getStatus() == AppointmentStatus.AVAILABLE) {
            throw new RuntimeException("Запис не заброньовано");
        }

        // 2) Перевірка допустимого вікна для клієнтів
        if (!isPrivileged()) {
            LocalDateTime now = LocalDateTime.now();
            if (now.isAfter(appointment.getAppointmentTime()
                    .minusHours(modificationWindowHours))) {
                throw new RuntimeException(
                        "Скасування можливе не пізніше ніж за "
                                + modificationWindowHours + " годин до прийому");
            }
            // рахуємо це як зміну
            trackChange(appointment);
        }

        calendarService.deleteAppointment(appointment);

        appointment.setClient(null);
        appointment.setStatus(AppointmentStatus.AVAILABLE);
        appointment.setReminderSent(false);
        return appointmentRepository.save(appointment);
    }

    @Override
    public List<TimeSlotDto> getWeeklyCalendar(Long dentistId, LocalDate weekStart) {
        int startHour = 6;
        int endHour = 22;

        User dentist = userRepository.findById(dentistId)
                .orElseThrow(() -> new RuntimeException("Dentist not found"));

        LocalDateTime periodStart = weekStart.atTime(startHour, 0);
        LocalDateTime periodEnd = weekStart.plusDays(6).atTime(endHour, 0);
        System.out.println("Fetching calendar for dentist: " + dentist.getEmail()
                + " from " + periodStart + " to " + periodEnd);
        List<Appointment> appointments = appointmentRepository.findByDentistAndAppointmentTimeBetween(
                dentist, periodStart, periodEnd);

        // Якщо потрібно повертати лише записи, де статус не AVAILABLE:
        return appointments.stream()
                .filter(appt -> appt.getStatus() != AppointmentStatus.AVAILABLE)
                .peek(System.out::println) // для налагодження
                .map(appt -> {
                    TimeSlotDto slotDto = new TimeSlotDto();
                    slotDto.setSlotTime(appt.getAppointmentTime());
                    slotDto.setStatus(appt.getStatus());
                    slotDto.setAppointmentId(appt.getId());
                    slotDto.setDurationMinutes(appt.getDurationMinutes());
                    if (appt.getClient() != null) {
                        slotDto.setClientId(appt.getClient().getId());
                        slotDto.setClientName(appt.getClient().getEmail());
                        slotDto.setFirstName(appt.getClient().getFirstName());
                        slotDto.setLastName(appt.getClient().getLastName());
                        slotDto.setComment(appt.getComment());
                    }
                    return slotDto;
                })
                .collect(Collectors.toList());
    }


    @Override
    @Transactional
    public Appointment bookSlot(Long dentistId,
                                LocalDateTime appointmentTime,
                                Long clientId,
                                Integer durationMinutes,
                                String comment) {
        User dentist = userRepository.findById(dentistId)
                .orElseThrow(() -> new RuntimeException("Dentist not found"));
        User client = userRepository.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Client not found"));

        // 1) Перевірка ролі стоматолога
        if (!dentist.getRole().equals(Role.DENTIST)) {
            throw new RuntimeException("Користувач не є стоматологом");
        }

        // 2) Ліміт 1 бронь на календарний день
        LocalDate date = appointmentTime.toLocalDate();
        LocalDateTime dayStart = date.atStartOfDay();
        LocalDateTime dayEnd = date.atTime(23, 59, 59);
        long dailyCount = appointmentRepository
                .countByClientAndStatusAndAppointmentTimeBetween(
                        client, AppointmentStatus.BOOKED, dayStart, dayEnd);
        if (dailyCount >= 1) {
            throw new RuntimeException("Ви не можете забронювати більше 1 прийому на цей день");
        }

        // 3) Ліміт 3 броні за останні 24 години
        LocalDate today = LocalDate.now();
        dayStart = today.atStartOfDay();
        dayEnd = today.plusDays(1).atStartOfDay().minusNanos(1);

        long createdToday = appointmentRepository
                .countByClientAndCreatedAtBetween(client, dayStart, dayEnd);

        if (createdToday >= settingsService.getSettings().booking24hLimit()) {
            throw new RuntimeException("Ви не можете забронювати більше " + settingsService.getSettings().booking24hLimit() + " прийомів за сьогодні");
        }

        // 4) Не більше одного “активного” бронювання в ту саму годину
        LocalDateTime hourStart = appointmentTime.truncatedTo(ChronoUnit.HOURS);
        LocalDateTime hourEnd = hourStart.plusHours(1).minusNanos(1);
        long overlap = appointmentRepository
                .countByClientAndStatusAndAppointmentTimeBetween(
                        client, AppointmentStatus.BOOKED, hourStart, hourEnd);
        if (overlap >= 1) {
            throw new RuntimeException("У вас уже є запис в цей же часовий слот");
        }

        // 5) Далі – як було
        Optional<Appointment> apptOpt = appointmentRepository
                .findByDentistAndAppointmentTime(dentist, appointmentTime);

        Appointment appointment = apptOpt.map(a -> {
            if (a.getStatus() != AppointmentStatus.AVAILABLE) {
                throw new RuntimeException("Слот недоступний для бронювання");
            }
            return a;
        }).orElseGet(Appointment::new);

        appointment.setDentist(dentist);
        appointment.setAppointmentTime(appointmentTime);
        appointment.setClient(client);
        appointment.setStatus(AppointmentStatus.BOOKED);
        appointment.setDurationMinutes(durationMinutes);
        appointment.setComment(comment);

        Appointment saved = appointmentRepository.save(appointment);
        calendarService.pushAppointment(saved);
        return saved;
    }

    @Override
    @Transactional
    public Appointment updateAppointment(Long appointmentId, LocalDateTime newAppointmentTime) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Запис не знайдено"));
        ClinicSettingsDto cfg = settingsService.getSettings();
        // наприклад:
        int modificationWindowHours = cfg.modificationWindowHours();
        int dailyChangeLimit = cfg.dailyChangeLimit();
        // 1) Дозволено оновлювати лише до модифікаційного вікна
        if (!isPrivileged()) {
            LocalDateTime now = LocalDateTime.now();
            if (now.isAfter(appointment.getAppointmentTime()
                    .minusHours(modificationWindowHours))) {
                throw new RuntimeException(
                        "Зміна часу можлива не пізніше ніж за "
                                + modificationWindowHours + " годин до прийому");
            }
            // 2) Перевірка ліміту змін на слот за поточну дату
            trackChange(appointment);
        }

        // 3) Конфлікти з іншими бронями (як раніше)
        User dentist = appointment.getDentist();
        appointmentRepository.findByDentistAndAppointmentTime(dentist, newAppointmentTime)
                .ifPresent(conflict -> {
                    if (!conflict.getId().equals(appointment.getId())) {
                        if (conflict.getStatus() == AppointmentStatus.BLOCKED) {
                            appointmentRepository.delete(conflict);
                        } else {
                            throw new RuntimeException("Слот вже заброньовано клієнтом");
                        }
                    }
                });

        // 4) Оновлюємо
        appointment.setAppointmentTime(newAppointmentTime);
        Appointment saved = appointmentRepository.save(appointment);
        if (saved.getStatus() == AppointmentStatus.BOOKED && saved.getClient() != null) {
            calendarService.pushAppointment(saved);
        }
        return saved;
    }

    /**
     * Інкрементує лічильник змін та збиває, якщо нова доба
     **/
    private void trackChange(Appointment appt) {
        ClinicSettingsDto cfg = settingsService.getSettings();
        // наприклад:
        int modificationWindowHours = cfg.modificationWindowHours();
        int dailyChangeLimit = cfg.dailyChangeLimit();
        LocalDate today = LocalDate.now();
        if (!today.equals(appt.getLastChangeDate())) {
            appt.setDailyChangeCount(0);
        }
        if (appt.getDailyChangeCount() >= dailyChangeLimit) {
            throw new RuntimeException(
                    "Досягнуто ліміту у " + dailyChangeLimit + " змін цього слоту за добу");
        }
        appt.setDailyChangeCount(appt.getDailyChangeCount() + 1);
        appt.setLastChangeDate(today);
    }

    @Override
    public UserAppointmentsDto getUserAppointmentsByTimeCategories(UserDto user) {
        User usr = userRepository.findById(user.getId()).orElseThrow(() -> new UserNotFoundException("User not found"));
        List<Appointment> allAppointments = appointmentRepository.findByClient(usr);

        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime tomorrowStart = today.plusDays(1).atStartOfDay();

        // Обчислюємо неділю поточного тижня та встановлюємо межу як кінець дня (23:59:59)
        LocalDate sunday = today.with(SUNDAY);
        LocalDateTime weekBoundary = sunday.atTime(23, 59, 59);

        // Минулі записи – до початку сьогоднішнього дня
        List<Appointment> pastAppointments = allAppointments.stream()
                .filter(appt -> appt.getAppointmentTime().isBefore(todayStart))
                .toList();

        // Записи на сьогодні – від початку сьогоднішнього дня до початку завтрашнього
        List<Appointment> todaysAppointments = allAppointments.stream()
                .filter(appt -> !appt.getAppointmentTime().isBefore(todayStart)
                        && appt.getAppointmentTime().isBefore(tomorrowStart))
                .toList();

        // Записи "цього тижня" – від початку завтрашнього дня до кінця неділі (включно)
        List<Appointment> thisWeekAppointments = allAppointments.stream()
                .filter(appt -> !appt.getAppointmentTime().isBefore(tomorrowStart)
                        && !appt.getAppointmentTime().isAfter(weekBoundary))
                .toList();

        // Майбутні записи – після кінця неділі
        List<Appointment> futureAppointments = allAppointments.stream()
                .filter(appt -> appt.getAppointmentTime().isAfter(weekBoundary))
                .toList();

        UserAppointmentsDto dto = new UserAppointmentsDto();
        dto.setPastAppointments(pastAppointments);
        dto.setTodaysAppointments(todaysAppointments);
        dto.setThisWeekAppointments(thisWeekAppointments);
        dto.setFutureAppointments(futureAppointments);

        return dto;
    }

    @Override
    @Transactional
    public Appointment updateAppointmentComment(Long appointmentId, String comment) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Запис не знайдено"));
        // Оновлюємо поле comment, яке може містити текст або бути очищеним (null)
        appointment.setComment(comment);
        return appointmentRepository.save(appointment);
    }

    @Transactional(readOnly = true)
    @Override
    public DentistStatisticsDto getDentistStatistics(Long dentistId,
                                                     String period,
                                                     LocalDate frameStart) {

        User dentist = userRepository.findById(dentistId)
                .orElseThrow(() -> new RuntimeException("Dentist not found"));

        // ---------- 1️⃣ визначаємо «start» ------------------
        LocalDate today = LocalDate.now();
        LocalDate start;
        period = period.toLowerCase();

        if ("week".equals(period)) {
            LocalDate base = (frameStart != null ? frameStart : today);
            start = base.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));   // ➡ понеділок
        } else if ("month".equals(period)) {
            LocalDate base = (frameStart != null ? frameStart : today);
            start = base.withDayOfMonth(1);
        } else {
            LocalDate base = (frameStart != null ? frameStart : today);
            start = base.withDayOfYear(1);
        }

        // ---------- 2️⃣ кінець відрізка ----------------------
        LocalDate end = switch (period) {
            case "week" -> start.plusDays(6);
            case "month" -> start.plusMonths(1).minusDays(1);
            case "year" -> start.plusYears(1).minusDays(1);
            default -> today;
        };

        LocalDateTime from = start.atStartOfDay();
        LocalDateTime to = end.atTime(23, 59, 59);

        // ---------- 3️⃣ дістаємо BOOKED-записи ---------------
        List<Appointment> list = appointmentRepository
                .findByDentistAndAppointmentTimeBetween(dentist, from, to)
                .stream()
                .filter(a -> a.getStatus() == AppointmentStatus.BOOKED)
                .toList();

        // ---------- 4️⃣ daily counts -------------------------
        Map<LocalDate, Long> daily = list.stream()
                .collect(Collectors.groupingBy(
                        a -> a.getAppointmentTime().toLocalDate(),
                        Collectors.counting()));

        long days = ChronoUnit.DAYS.between(start, end) + 1;
        List<DailyCountDto> dailyCounts = Stream.iterate(start, d -> d.plusDays(1))
                .limit(days)
                .map(d -> new DailyCountDto(d, daily.getOrDefault(d, 0L)))
                .toList();

        // ---------- 5️⃣ hourly counts ------------------------
        Map<Integer, Long> hourly = list.stream()
                .collect(Collectors.groupingBy(
                        a -> a.getAppointmentTime().getHour(),
                        Collectors.counting()));

        List<HourlyCountDto> hourlyCounts = IntStream.range(0, 24)
                .mapToObj(h -> new HourlyCountDto(h, hourly.getOrDefault(h, 0L)))
                .toList();

        double avgDuration = list.stream()
                .mapToDouble(Appointment::getDurationMinutes)
                .average()
                .orElse(0.0);

// ---------- 6️⃣ fill the DTO ----------------
        DentistStatisticsDto dto = new DentistStatisticsDto();
        dto.setDailyCounts(dailyCounts);
        dto.setHourlyCounts(hourlyCounts);
        dto.setTotalCompletedAppointments(list.size());
        dto.setAverageDurationMinutes(avgDuration);

        return dto;
    }

    @Override
    public List<NextFreeSlotDto> getNextFreeSlotsForAllDentists(int requiredMinutes) {
        LocalDate today = LocalDate.now();
        List<User> dentists = userRepository.findByRole(Role.DENTIST);
        List<NextFreeSlotDto> out = new ArrayList<>();

        for (User d : dentists) {
            List<TimeSlotDto> week = this.getWeeklyCalendar(d.getId(), today);
            week.stream()
                    .filter(ts -> ts.getStatus() == AppointmentStatus.AVAILABLE)
                    .filter(ts -> ts.getDurationMinutes() >= requiredMinutes)
                    .findFirst()
                    .ifPresent(ts -> out.add(new NextFreeSlotDto(
                            d.getId(),
                            d.getFirstName() + " " + d.getLastName(),
                            ts.getSlotTime(),
                            ts.getDurationMinutes()
                    )));
        }
        return out;
    }

    @Override
    @Transactional(readOnly = true)
    public List<NextFreeSlotDto> getNextFreeSlots(int requiredMinutes) {
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();

        // look 7 days ahead
        LocalDate endDay = today.plusDays(7);

        // all your dentists
        List<User> dentists = userRepository.findByRole(Role.DENTIST);
        List<NextFreeSlotDto> out = new ArrayList<>();

        for (User dentist : dentists) {
            // 1) get all occupied times (BOOKED or BLOCKED) in the 7-day window
            LocalDateTime windowStart = today.atStartOfDay();
            LocalDateTime windowEnd = endDay.atTime(23, 59, 59);

            Set<LocalDateTime> occupied = appointmentRepository
                    .findByDentistAndAppointmentTimeBetween(dentist, windowStart, windowEnd)
                    .stream()
                    .filter(a -> a.getStatus() != AppointmentStatus.AVAILABLE)
                    .map(Appointment::getAppointmentTime)
                    .collect(Collectors.toSet());

            // 2) scan *day by day* only during working hours
            LocalDate scanDate = today;
            boolean found = false;
            outer:
            while (!found && !scanDate.isAfter(endDay)) {
                for (int hour = workStartHour; hour <= workEndHour; hour++) {
                    LocalDateTime slot = scanDate.atTime(hour, 0);
                    // skip past
                    if (slot.isBefore(now)) continue;
                    // ensure it will fit before midnight (you can also check slot.plusMinutes(requiredMinutes) <= …)
                    // here we assume requiredMinutes <= (24-hour)
                    if (!occupied.contains(slot)) {
                        // bingo—first free hour for this dentist
                        out.add(new NextFreeSlotDto(
                                dentist.getId(),
                                dentist.getFirstName() + " " + dentist.getLastName(),
                                slot,
                                requiredMinutes
                        ));
                        found = true;
                        break outer;
                    }
                }
                scanDate = scanDate.plusDays(1);
            }
        }

        return out;
    }


    @Override
    @Transactional(readOnly = true)
    public List<Appointment> getPatientAppointmentHistory(Long patientId, int page, int size) {
        User patient = userRepository.findById(patientId)
                .orElseThrow(() -> new RuntimeException("Patient not found"));

        // Отримуємо всі завершені записи пацієнта, відсортовані за датою (найновіші першими)
        List<Appointment> allAppointments = appointmentRepository.findByClient(patient)
                .stream()
                .filter(appt -> appt.getAppointmentTime().isBefore(LocalDateTime.now())) // Тільки минулі
                .sorted((a, b) -> b.getAppointmentTime().compareTo(a.getAppointmentTime())) // Найновіші першими
                .toList();

        // Простий пагінація (без Spring Data)
        int start = page * size;
        int end = Math.min(start + size, allAppointments.size());

        if (start >= allAppointments.size()) {
            return new ArrayList<>();
        }

        return allAppointments.subList(start, end);
    }

    @Override
    @Transactional(readOnly = true)
    public long getPatientAppointmentCount(Long patientId) {
        User patient = userRepository.findById(patientId)
                .orElseThrow(() -> new RuntimeException("Patient not found"));

        return appointmentRepository.findByClient(patient)
                .stream()
                .filter(appt -> appt.getAppointmentTime().isBefore(LocalDateTime.now())) // Тільки минулі
                .count();
    }

}
