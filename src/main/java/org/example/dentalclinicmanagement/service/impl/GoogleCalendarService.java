package org.example.dentalclinicmanagement.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.dentalclinicmanagement.model.Appointment;
import org.example.dentalclinicmanagement.model.AppointmentCalendarLink;
import org.example.dentalclinicmanagement.model.GoogleCredential;
import org.example.dentalclinicmanagement.model.User;
import org.example.dentalclinicmanagement.repository.AppointmentCalendarLinkRepository;
import org.example.dentalclinicmanagement.repository.GoogleCredentialRepository;
import org.example.dentalclinicmanagement.repository.UserRepository;
import org.example.dentalclinicmanagement.service.CalendarService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.*;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.UserCredentials;

import java.time.ZoneId;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class GoogleCalendarService implements CalendarService {

    private final GoogleCredentialRepository credRepo;
    private final AppointmentCalendarLinkRepository linkRepo;
    private final UserRepository userRepo;
    private final Logger log = LoggerFactory.getLogger(getClass());

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String clientId;
    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String clientSecret;

    @Override
    public void pushAppointment(Appointment appointment) {
        log.info("⏩  Календар: апойнтмент {}, clientId={}",
                appointment.getId(), appointment.getClient().getId());

        if (appointment.getClient() == null) return;           // без клієнта — вихід
        var credOpt = credRepo.findByUser(appointment.getClient());
        if (credOpt.isEmpty()) return;                         // Google не під’єднано

        // Java
        try {
            Calendar calendar = buildCalendar(credOpt.get());
            // Check if an event already exists; otherwise insert a new event
            var linkOpt = linkRepo.findByAppointmentId(appointment.getId());
            if (linkOpt.isPresent()) {
                updateEvent(calendar, linkOpt.get().getGoogleEventId(), appointment);
                log.info("Google event updated (eventId {})", linkOpt.get().getGoogleEventId());
            } else {
                Event created = calendar.events()
                        .insert("primary", toEvent(appointment))
                        .execute();
                AppointmentCalendarLink l = new AppointmentCalendarLink();
                l.setAppointmentId(appointment.getId());
                l.setGoogleEventId(created.getId());
                l.setUserId(appointment.getClient().getId());
                linkRepo.save(l);
                log.info("Google event created (eventId {})", created.getId());
            }
        } catch (com.google.api.client.googleapis.json.GoogleJsonResponseException ex) {
            if (ex.getStatusCode() == 403) {
                log.error("User did not grant enough permissions for Calendar access. Please update scopes and re-authenticate.", ex);
                // Optionally, trigger a flow to request updated permissions from the user.
            } else {
                log.error("Error synchronizing Google Calendar", ex);
            }
        } catch (Exception ex) {
            log.error("Error synchronizing Google Calendar", ex);
        }
    }

    public boolean hasCalendarAccess(User user) {
        var credOpt = credRepo.findByUser(user);
        if (credOpt.isEmpty()) return false;

        try {
            Calendar calendar = buildCalendar(credOpt.get());
            // просто спробуємо витягнути одну подію (як тест)
            calendar.events()
                    .list("primary")
                    .setMaxResults(1)
                    .execute();
            return true;
        } catch (Exception ex) {
            log.warn("❌ Google Calendar access test failed for user {}, reason: {}", user.getEmail(), ex.getMessage());
            return false;
        }
    }

    @Override
    public void deleteAppointment(Appointment appt) {
        linkRepo.findByAppointmentId(appt.getId()).ifPresent(link -> {
            User user = userRepo.findById(link.getUserId())
                    .orElseThrow();               // не може статись
            credRepo.findByUser(user).ifPresent(cred -> {
                try {
                    Calendar cal = buildCalendar(cred);
                    cal.events()
                            .delete("primary", link.getGoogleEventId())
                            .execute();                            // 🗑️
                    linkRepo.delete(link);                    // прибираємо з БД
                    log.info("🗑️  Google-подію {} видалено", link.getGoogleEventId());
                } catch (Exception ex) {
                    log.error("❌  Помилка видалення події", ex);
                }
            });
        });
    }


    // ------------ helpers -------------
    private Calendar buildCalendar(GoogleCredential cred) throws Exception {
        // 1. Формуємо AccessToken
        AccessToken at = new AccessToken(
                cred.getAccessToken(),
                Date.from(cred.getAccessTokenExpiry())
        );

        // 2. Створюємо UserCredentials
        UserCredentials userCred = UserCredentials.newBuilder()
                .setClientId(clientId)
                .setClientSecret(clientSecret)
                .setRefreshToken(cred.getRefreshToken())
                .setAccessToken(at)
                .build();

        // 3. Обгортка під HttpRequestInitializer
        HttpRequestInitializer reqInit = new HttpCredentialsAdapter(userCred);

        // 4. Будуємо Calendar
        return new Calendar.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                JacksonFactory.getDefaultInstance(),
                reqInit)
                .setApplicationName("DentalClinicManagement")
                .build();
    }

    private Event toEvent(Appointment a) {
        java.time.ZonedDateTime startZdt = a.getAppointmentTime().atZone(ZoneId.systemDefault());
        java.time.ZonedDateTime endZdt = startZdt.plusMinutes(a.getDurationMinutes() != null ? a.getDurationMinutes() : 30);

        EventDateTime start = new EventDateTime()
                .setDateTime(new com.google.api.client.util.DateTime(Date.from(startZdt.toInstant())))
                .setTimeZone(startZdt.getZone().toString());
        EventDateTime end = new EventDateTime()
                .setDateTime(new com.google.api.client.util.DateTime(Date.from(endZdt.toInstant())))
                .setTimeZone(endZdt.getZone().toString());

        return new Event()
                .setSummary("Візит до стоматолога")
                .setDescription(a.getComment() == null ? "" : a.getComment())
                .setStart(start)
                .setEnd(end)
                .setReminders(new Event.Reminders().setUseDefault(true));
    }

    private void updateEvent(Calendar cal, String eventId, Appointment a) throws Exception {
        Event event = cal.events().get("primary", eventId).execute();
        Event updated = toEvent(a);
        // замінюємо час та опис
        event.setStart(updated.getStart())
                .setEnd(updated.getEnd())
                .setDescription(updated.getDescription());
        cal.events().update("primary", eventId, event).execute();
    }
}
