package org.example.dentalclinicmanagement.service.impl;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.UserCredentials;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.dentalclinicmanagement.dto.CalendarEventDto;
import org.example.dentalclinicmanagement.model.Appointment;
import org.example.dentalclinicmanagement.model.AppointmentCalendarLink;
import org.example.dentalclinicmanagement.model.GoogleCredential;
import org.example.dentalclinicmanagement.model.User;
import org.example.dentalclinicmanagement.repository.AppointmentCalendarLinkRepository;
import org.example.dentalclinicmanagement.repository.GoogleCredentialRepository;
import org.example.dentalclinicmanagement.service.CalendarService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Date;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class GoogleCalendarServiceImpl implements CalendarService {

    private final GoogleCredentialRepository credentialRepository;
    private final AppointmentCalendarLinkRepository linkRepository;

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String clientSecret;

    private static final String APPLICATION_NAME = "Dental Clinic Management";
    private static final String CALENDAR_ID = "primary";

    @Override
    @Transactional
    public CalendarEventDto syncAppointment(Appointment appointment) {
        if (appointment.getClient() == null) {
            log.debug("No client for appointment {}, skipping calendar sync", appointment.getId());
            return null;
        }

        log.info("Syncing appointment {} to Google Calendar for client {}",
                appointment.getId(), appointment.getClient().getId());

        try {
            Optional<GoogleCredential> credentialOpt = credentialRepository.findByUser(appointment.getClient());
            if (credentialOpt.isEmpty()) {
                log.debug("No Google credentials found for user {}", appointment.getClient().getId());
                return null;
            }

            Calendar calendar = buildCalendar(credentialOpt.get());
            Optional<AppointmentCalendarLink> linkOpt = linkRepository.findByAppointmentId(appointment.getId());

            if (linkOpt.isPresent()) {
                return updateExistingEvent(calendar, linkOpt.get(), appointment);
            } else {
                return createNewEvent(calendar, appointment);
            }

        } catch (Exception ex) {
            log.error("Error syncing appointment {} to Google Calendar: {}",
                    appointment.getId(), ex.getMessage(), ex);
            return null;
        }
    }

    @Override
    @Transactional
    public boolean removeAppointment(Appointment appointment) {
        log.info("Deleting appointment {} from Google Calendar", appointment.getId());

        try {
            Optional<AppointmentCalendarLink> linkOpt = linkRepository.findByAppointmentId(appointment.getId());
            if (linkOpt.isEmpty()) {
                log.debug("No calendar link found for appointment {}", appointment.getId());
                return true;
            }

            AppointmentCalendarLink link = linkOpt.get();
            Optional<GoogleCredential> credentialOpt = credentialRepository.findByUser_Id(link.getUserId());
            
            if (credentialOpt.isEmpty()) {
                log.debug("No Google credentials found for user {}", link.getUserId());
                linkRepository.delete(link);
                return true;
            }

            Calendar calendar = buildCalendar(credentialOpt.get());
            calendar.events().delete(CALENDAR_ID, link.getGoogleEventId()).execute();
            linkRepository.delete(link);

            log.info("Google event deleted: eventId={}", link.getGoogleEventId());
            return true;

        } catch (Exception ex) {
            log.error("Error deleting appointment {} from Google Calendar: {}",
                    appointment.getId(), ex.getMessage(), ex);
            return false;
        }
    }

    @Override
    @Transactional
    public void storeCredentials(User user, String accessToken, String refreshToken, Long expiresAt) {
        GoogleCredential credential = credentialRepository.findByUser(user)
                .orElse(new GoogleCredential());
        
        credential.setUser(user);
        credential.setAccessToken(accessToken);
        credential.setRefreshToken(refreshToken);
        credential.setAccessTokenExpiry(Instant.ofEpochMilli(expiresAt));
        
        credentialRepository.save(credential);
        log.info("Google credentials stored for user: {}", user.getEmail());
    }

    private Calendar buildCalendar(GoogleCredential credential) throws Exception {
        NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        GsonFactory jsonFactory = GsonFactory.getDefaultInstance();

        AccessToken accessToken = new AccessToken(
                credential.getAccessToken(),
                Date.from(credential.getAccessTokenExpiry())
        );

        UserCredentials userCredentials = UserCredentials.newBuilder()
                .setClientId(clientId)
                .setClientSecret(clientSecret)
                .setRefreshToken(credential.getRefreshToken())
                .setAccessToken(accessToken)
                .build();

        return new Calendar.Builder(httpTransport, jsonFactory, new HttpCredentialsAdapter(userCredentials))
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    private CalendarEventDto createNewEvent(Calendar calendar, Appointment appointment) throws Exception {
        Event event = buildEvent(appointment);
        Event created = calendar.events().insert(CALENDAR_ID, event).execute();

        AppointmentCalendarLink link = new AppointmentCalendarLink();
        link.setAppointmentId(appointment.getId());
        link.setGoogleEventId(created.getId());
        link.setUserId(appointment.getClient().getId());
        linkRepository.save(link);

        log.info("Google event created: eventId={}", created.getId());
        
        return CalendarEventDto.builder()
                .eventId(created.getId())
                .title(created.getSummary())
                .description(created.getDescription())
                .status("created")
                .build();
    }

    private CalendarEventDto updateExistingEvent(Calendar calendar, AppointmentCalendarLink link, 
                                                Appointment appointment) throws Exception {
        try {
            Event existingEvent = calendar.events().get(CALENDAR_ID, link.getGoogleEventId()).execute();
            Event updatedEvent = buildEvent(appointment);

            existingEvent.setStart(updatedEvent.getStart())
                    .setEnd(updatedEvent.getEnd())
                    .setDescription(updatedEvent.getDescription())
                    .setSummary(updatedEvent.getSummary());

            Event updated = calendar.events().update(CALENDAR_ID, link.getGoogleEventId(), existingEvent).execute();
            
            log.info("Google event updated: eventId={}", link.getGoogleEventId());
            
            return CalendarEventDto.builder()
                    .eventId(updated.getId())
                    .title(updated.getSummary())
                    .description(updated.getDescription())
                    .status("updated")
                    .build();
                    
        } catch (Exception ex) {
            log.warn("Failed to update existing event {}, creating new one", link.getGoogleEventId());
            linkRepository.delete(link);
            return createNewEvent(calendar, appointment);
        }
    }

    private Event buildEvent(Appointment appointment) {
        ZoneId systemZone = ZoneId.systemDefault();
        var startZdt = appointment.getAppointmentTime().atZone(systemZone);
        var endZdt = startZdt.plusMinutes(
                appointment.getDurationMinutes() != null ? appointment.getDurationMinutes() : 30);

        EventDateTime start = new EventDateTime()
                .setDateTime(new com.google.api.client.util.DateTime(Date.from(startZdt.toInstant())))
                .setTimeZone(systemZone.toString());

        EventDateTime end = new EventDateTime()
                .setDateTime(new com.google.api.client.util.DateTime(Date.from(endZdt.toInstant())))
                .setTimeZone(systemZone.toString());

        String dentistName = appointment.getDentist().getFirstName() + " " + appointment.getDentist().getLastName();
        String summary = "Dental Appointment with Dr. " + dentistName;
        String description = buildEventDescription(appointment);

        return new Event()
                .setSummary(summary)
                .setDescription(description)
                .setStart(start)
                .setEnd(end)
                .setReminders(new Event.Reminders().setUseDefault(true));
    }

    private String buildEventDescription(Appointment appointment) {
        StringBuilder description = new StringBuilder();
        description.append("Dental clinic appointment\n");
        description.append("Doctor: Dr. ").append(appointment.getDentist().getFirstName())
                   .append(" ").append(appointment.getDentist().getLastName()).append("\n");
        description.append("Duration: ").append(appointment.getDurationMinutes()).append(" minutes\n");
        
        if (appointment.getComment() != null && !appointment.getComment().trim().isEmpty()) {
            description.append("Notes: ").append(appointment.getComment());
        }
        
        return description.toString();
    }
}