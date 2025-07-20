package org.example.dentalclinicmanagement.service.impl;

import com.google.api.services.calendar.model.Event;
import org.example.dentalclinicmanagement.dto.CalendarEventDto;
import org.example.dentalclinicmanagement.model.*;
import org.example.dentalclinicmanagement.repository.AppointmentCalendarLinkRepository;
import org.example.dentalclinicmanagement.repository.GoogleCredentialRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GoogleCalendarServiceImplTest {

    @Mock
    private GoogleCredentialRepository credentialRepository;

    @Mock
    private AppointmentCalendarLinkRepository linkRepository;

    @InjectMocks
    private GoogleCalendarServiceImpl calendarService;

    private User client;
    private User dentist;
    private Appointment appointment;
    private GoogleCredential credential;
    private AppointmentCalendarLink calendarLink;
    private Event googleEvent;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(calendarService, "clientId", "test-client-id");
        ReflectionTestUtils.setField(calendarService, "clientSecret", "test-client-secret");

        client = new User();
        client.setId(1L);
        client.setEmail("client@example.com");
        client.setFirstName("John");
        client.setLastName("Doe");

        dentist = new User();
        dentist.setId(2L);
        dentist.setEmail("dentist@example.com");
        dentist.setFirstName("Dr. Jane");
        dentist.setLastName("Smith");

        appointment = new Appointment();
        appointment.setId(1L);
        appointment.setClient(client);
        appointment.setDentist(dentist);
        appointment.setAppointmentTime(LocalDateTime.now().plusDays(1));
        appointment.setDurationMinutes(30);
        appointment.setStatus(AppointmentStatus.BOOKED);
        appointment.setComment("Regular checkup");

        credential = new GoogleCredential();
        credential.setUser(client);
        credential.setAccessToken("access-token");
        credential.setRefreshToken("refresh-token");
        credential.setAccessTokenExpiry(Instant.now().plus(1, ChronoUnit.HOURS));

        calendarLink = new AppointmentCalendarLink();
        calendarLink.setAppointmentId(1L);
        calendarLink.setGoogleEventId("google-event-id");
        calendarLink.setUserId(1L);

        googleEvent = new Event();
        googleEvent.setId("google-event-id");
        googleEvent.setSummary("Dental Appointment with Dr. Jane Smith");
        googleEvent.setDescription("Test appointment");
    }

    @Test
    void syncAppointment_NoClient_ReturnsNull() {
        appointment.setClient(null);

        CalendarEventDto result = calendarService.syncAppointment(appointment);

        assertNull(result);
        verify(credentialRepository, never()).findByUser(any());
    }

    @Test
    void syncAppointment_NoCredentials_ReturnsNull() {
        when(credentialRepository.findByUser(client)).thenReturn(Optional.empty());

        CalendarEventDto result = calendarService.syncAppointment(appointment);

        assertNull(result);
        verify(credentialRepository).findByUser(client);
        verify(linkRepository, never()).findByAppointmentId(any());
    }

    @Test
    void removeAppointment_NoCalendarLink_ReturnsTrue() {
        when(linkRepository.findByAppointmentId(1L)).thenReturn(Optional.empty());

        boolean result = calendarService.removeAppointment(appointment);

        assertTrue(result);
        verify(linkRepository).findByAppointmentId(1L);
        verify(credentialRepository, never()).findByUser_Id(any());
    }

    @Test
    void removeAppointment_NoCredentials_DeletesLinkAndReturnsTrue() {
        when(linkRepository.findByAppointmentId(1L)).thenReturn(Optional.of(calendarLink));
        when(credentialRepository.findByUser_Id(1L)).thenReturn(Optional.empty());

        boolean result = calendarService.removeAppointment(appointment);

        assertTrue(result);
        verify(linkRepository).delete(calendarLink);
    }

    @Test
    void storeCredentials_NewCredential_CreatesNew() {
        when(credentialRepository.findByUser(client)).thenReturn(Optional.empty());
        when(credentialRepository.save(any(GoogleCredential.class))).thenReturn(credential);

        calendarService.storeCredentials(client, "access-token", "refresh-token", 3600000L);

        verify(credentialRepository).save(argThat(cred ->
                cred.getUser().equals(client) &&
                        cred.getAccessToken().equals("access-token") &&
                        cred.getRefreshToken().equals("refresh-token")
        ));
    }

    @Test
    void storeCredentials_ExistingCredential_Updates() {
        when(credentialRepository.findByUser(client)).thenReturn(Optional.of(credential));
        when(credentialRepository.save(any(GoogleCredential.class))).thenReturn(credential);

        calendarService.storeCredentials(client, "new-access-token", "new-refresh-token", 7200000L);

        verify(credentialRepository).save(argThat(cred ->
                cred.getUser().equals(client) &&
                        cred.getAccessToken().equals("new-access-token") &&
                        cred.getRefreshToken().equals("new-refresh-token")
        ));
    }

    @Test
    void hasCalendarAccess_NoCredentials_ReturnsFalse() {
        when(credentialRepository.findByUser(client)).thenReturn(Optional.empty());

        boolean result = calendarService.hasCalendarAccess(client);

        assertFalse(result);
        verify(credentialRepository).findByUser(client);
    }

    @Test
    void syncAppointment_ExceptionDuringSync_ReturnsNull() {
        when(credentialRepository.findByUser(client)).thenReturn(Optional.of(credential));
        when(linkRepository.findByAppointmentId(1L)).thenThrow(new RuntimeException("Database error"));

        CalendarEventDto result = calendarService.syncAppointment(appointment);

        assertNull(result);
    }

    @Test
    void removeAppointment_CleansUpOrphanedLink() {
        when(linkRepository.findByAppointmentId(1L)).thenReturn(Optional.of(calendarLink));
        when(credentialRepository.findByUser_Id(1L)).thenReturn(Optional.empty());

        boolean result = calendarService.removeAppointment(appointment);

        assertTrue(result);
        verify(linkRepository).delete(calendarLink);
    }

    @Test
    void storeCredentials_HandlesLongExpiration() {
        long futureExpiration = Instant.now().plus(365, ChronoUnit.DAYS).toEpochMilli();
        when(credentialRepository.findByUser(client)).thenReturn(Optional.empty());
        when(credentialRepository.save(any(GoogleCredential.class))).thenReturn(credential);

        calendarService.storeCredentials(client, "access-token", "refresh-token", futureExpiration);

        verify(credentialRepository).save(argThat(cred ->
                cred.getAccessTokenExpiry().isAfter(Instant.now().plus(300, ChronoUnit.DAYS))
        ));
    }

    @Test
    void storeCredentials_HandlesExpiredToken() {
        long pastExpiration = Instant.now().minus(1, ChronoUnit.HOURS).toEpochMilli();
        when(credentialRepository.findByUser(client)).thenReturn(Optional.empty());
        when(credentialRepository.save(any(GoogleCredential.class))).thenReturn(credential);

        calendarService.storeCredentials(client, "access-token", "refresh-token", pastExpiration);

        verify(credentialRepository).save(argThat(cred ->
                cred.getAccessTokenExpiry().isBefore(Instant.now())
        ));
    }

    @Test
    void hasCalendarAccess_CatchesAndHandlesExceptions() {
        when(credentialRepository.findByUser(client)).thenReturn(Optional.of(credential));

        boolean result = calendarService.hasCalendarAccess(client);

        assertNotNull(result);
    }
}