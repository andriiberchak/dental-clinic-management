package org.example.dentalclinicmanagement.service.impl;

import org.example.dentalclinicmanagement.dto.AppointmentDto;
import org.example.dentalclinicmanagement.dto.ClinicSettingsDto;
import org.example.dentalclinicmanagement.dto.UserAppointmentsDto;
import org.example.dentalclinicmanagement.dto.request.BookSlotRequest;
import org.example.dentalclinicmanagement.dto.request.CreateSlotRequest;
import org.example.dentalclinicmanagement.dto.request.UpdateAppointmentRequest;
import org.example.dentalclinicmanagement.exception.AppointmentException;
import org.example.dentalclinicmanagement.exception.UserNotFoundException;
import org.example.dentalclinicmanagement.mapper.AppointmentMapper;
import org.example.dentalclinicmanagement.model.*;
import org.example.dentalclinicmanagement.repository.AppointmentRepository;
import org.example.dentalclinicmanagement.repository.UserRepository;
import org.example.dentalclinicmanagement.service.CalendarService;
import org.example.dentalclinicmanagement.service.ClinicSettingsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceImplTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ClinicSettingsService settingsService;

    @Mock
    private CalendarService calendarService;

    @Mock
    private AppointmentMapper appointmentMapper;

    @InjectMocks
    private AppointmentServiceImpl appointmentService;

    private User dentist;
    private User client;
    private Appointment appointment;
    private AppointmentDto appointmentDto;
    private CreateSlotRequest createSlotRequest;
    private BookSlotRequest bookSlotRequest;
    private UpdateAppointmentRequest updateRequest;
    private ClinicSettingsDto settings;

    @BeforeEach
    void setUp() {
        dentist = new User();
        dentist.setId(1L);
        dentist.setEmail("dentist@example.com");
        dentist.setFirstName("Dr. John");
        dentist.setLastName("Smith");
        dentist.setRole(Role.DENTIST);

        client = new User();
        client.setId(2L);
        client.setEmail("client@example.com");
        client.setFirstName("Jane");
        client.setLastName("Doe");
        client.setRole(Role.USER);

        appointment = new Appointment();
        appointment.setId(1L);
        appointment.setDentist(dentist);
        appointment.setClient(client);
        appointment.setAppointmentTime(LocalDateTime.now().plusDays(1));
        appointment.setStatus(AppointmentStatus.BOOKED);
        appointment.setDurationMinutes(30);

        appointmentDto = AppointmentDto.builder().id(1L).build();
        appointmentDto.setId(1L);

        createSlotRequest = new CreateSlotRequest();
        createSlotRequest.setAppointmentTime(LocalDateTime.now().plusDays(1));
        createSlotRequest.setStatus(AppointmentStatus.AVAILABLE);
        createSlotRequest.setDurationMinutes(30);

        bookSlotRequest = new BookSlotRequest();
        bookSlotRequest.setDentistId(1L);
        bookSlotRequest.setClientId(2L);
        bookSlotRequest.setSlotTime(LocalDateTime.now().plusDays(1));
        bookSlotRequest.setDurationMinutes(30);
        bookSlotRequest.setComment("Test appointment");

        updateRequest = new UpdateAppointmentRequest();
        updateRequest.setNewTime(LocalDateTime.now().plusDays(2));

        settings = new ClinicSettingsDto(24, 3, 5, 10, 1);

        ReflectionTestUtils.setField(appointmentService, "workStartHour", 9);
        ReflectionTestUtils.setField(appointmentService, "workEndHour", 20);
    }

    @Test
    void createAppointmentSlot_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(dentist));
        when(appointmentRepository.findByDentistAndAppointmentTime(dentist, createSlotRequest.getAppointmentTime()))
                .thenReturn(Optional.empty());
        when(appointmentRepository.save(any(Appointment.class))).thenReturn(appointment);
        when(appointmentMapper.toDto(appointment)).thenReturn(appointmentDto);

        AppointmentDto result = appointmentService.createAppointmentSlot(1L, createSlotRequest);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(userRepository).findById(1L);
        verify(appointmentRepository).save(any(Appointment.class));
        verify(appointmentMapper).toDto(appointment);
    }

    @Test
    void createAppointmentSlot_UserNotDentist_ThrowsException() {
        User nonDentist = new User();
        nonDentist.setId(1L);
        nonDentist.setRole(Role.USER);

        when(userRepository.findById(1L)).thenReturn(Optional.of(nonDentist));

        AppointmentException exception = assertThrows(
                AppointmentException.class,
                () -> appointmentService.createAppointmentSlot(1L, createSlotRequest)
        );

        assertEquals("User is not a dentist", exception.getMessage());
        verify(appointmentRepository, never()).save(any());
    }

    @Test
    void bookSlot_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(dentist));
        when(userRepository.findById(2L)).thenReturn(Optional.of(client));
        when(appointmentRepository.findByDentistAndAppointmentTime(dentist, bookSlotRequest.getSlotTime()))
                .thenReturn(Optional.empty());
        when(settingsService.getSettings()).thenReturn(settings);
        when(appointmentRepository.countByClientAndStatusAndAppointmentTimeBetween(any(), any(), any(), any()))
                .thenReturn(0L);
        when(appointmentRepository.countByClientAndCreatedAtBetween(any(), any(), any()))
                .thenReturn(0L);
        when(appointmentRepository.save(any(Appointment.class))).thenReturn(appointment);
        when(appointmentMapper.toDto(appointment)).thenReturn(appointmentDto);

        AppointmentDto result = appointmentService.bookSlot(bookSlotRequest, "client@example.com");

        assertNotNull(result);
        verify(appointmentRepository).save(any(Appointment.class));
        verify(calendarService).syncAppointment(appointment);
    }

    @Test
    void bookSlot_UnauthorizedUser_ThrowsException() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(dentist));
        when(userRepository.findById(2L)).thenReturn(Optional.of(client));

        AppointmentException exception = assertThrows(
                AppointmentException.class,
                () -> appointmentService.bookSlot(bookSlotRequest, "other@example.com")
        );

        assertEquals("You can only book appointments for yourself", exception.getMessage());
    }

    @Test
    void cancelAppointment_Success() {
        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(appointment));
        when(settingsService.getSettings()).thenReturn(settings);
        when(appointmentRepository.save(any(Appointment.class))).thenReturn(appointment);

        appointmentService.cancelAppointment(1L, "client@example.com");

        verify(calendarService).removeAppointment(appointment);
        verify(appointmentRepository).save(argThat(appt ->
                appt.getStatus() == AppointmentStatus.AVAILABLE &&
                        appt.getClient() == null
        ));
    }

    @Test
    void cancelAppointment_NotBooked_ThrowsException() {
        appointment.setStatus(AppointmentStatus.AVAILABLE);
        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(appointment));

        AppointmentException exception = assertThrows(
                AppointmentException.class,
                () -> appointmentService.cancelAppointment(1L, "client@example.com")
        );

        assertEquals("Appointment is not booked", exception.getMessage());
    }

    @Test
    void updateAppointment_Success() {
        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(appointment));
        when(appointmentRepository.findByDentistAndAppointmentTime(dentist, updateRequest.getNewTime()))
                .thenReturn(Optional.empty());
        when(settingsService.getSettings()).thenReturn(settings);
        when(appointmentRepository.save(any(Appointment.class))).thenReturn(appointment);
        when(appointmentMapper.toDto(appointment)).thenReturn(appointmentDto);

        AppointmentDto result = appointmentService.updateAppointment(1L, updateRequest, "client@example.com");

        assertNotNull(result);
        verify(appointmentRepository).save(any(Appointment.class));
        verify(calendarService).syncAppointment(appointment);
    }

    @Test
    void updateAppointmentComment_Success() {
        String newComment = "Updated comment";
        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(appointment));
        when(appointmentRepository.save(any(Appointment.class))).thenReturn(appointment);

        appointmentService.updateAppointmentComment(1L, newComment, "client@example.com");

        verify(appointmentRepository).save(argThat(appt ->
                newComment.equals(appt.getComment())
        ));
        verify(calendarService).syncAppointment(appointment);
    }

    @Test
    void getUserAppointmentsByTimeCategories_Success() {
        when(userRepository.findByEmail("client@example.com")).thenReturn(Optional.of(client));
        when(appointmentRepository.findByClient(client)).thenReturn(List.of(appointment));
        when(appointmentMapper.toDto(appointment)).thenReturn(appointmentDto);

        UserAppointmentsDto result = appointmentService.getUserAppointmentsByTimeCategories("client@example.com");

        assertNotNull(result);
        assertNotNull(result.getPastAppointments());
        assertNotNull(result.getTodaysAppointments());
        assertNotNull(result.getThisWeekAppointments());
        assertNotNull(result.getFutureAppointments());
    }

    @Test
    void findUserById_NotFound_ThrowsException() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        UserNotFoundException exception = assertThrows(
                UserNotFoundException.class,
                () -> appointmentService.createAppointmentSlot(999L, createSlotRequest)
        );

        assertEquals("User not found with id: 999", exception.getMessage());
    }

    @Test
    void findAppointmentById_NotFound_ThrowsException() {
        when(appointmentRepository.findById(999L)).thenReturn(Optional.empty());

        AppointmentException exception = assertThrows(
                AppointmentException.class,
                () -> appointmentService.cancelAppointment(999L, "client@example.com")
        );

        assertEquals("Appointment not found with id: 999", exception.getMessage());
    }

    @Test
    void validateBookingConstraints_DailyLimitExceeded_ThrowsException() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(dentist));
        when(userRepository.findById(2L)).thenReturn(Optional.of(client));
        when(settingsService.getSettings()).thenReturn(settings);
        when(appointmentRepository.countByClientAndStatusAndAppointmentTimeBetween(any(), any(), any(), any()))
                .thenReturn(6L);
        AppointmentException exception = assertThrows(
                AppointmentException.class,
                () -> appointmentService.bookSlot(bookSlotRequest, "client@example.com")
        );

        assertEquals("Daily booking limit exceeded", exception.getMessage());
    }
}