package org.example.dentalclinicmanagement.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.dentalclinicmanagement.dto.response.MessageResponse;
import org.example.dentalclinicmanagement.model.GoogleCredential;
import org.example.dentalclinicmanagement.model.User;
import org.example.dentalclinicmanagement.repository.AppointmentCalendarLinkRepository;
import org.example.dentalclinicmanagement.repository.GoogleCredentialRepository;
import org.example.dentalclinicmanagement.repository.UserRepository;
import org.example.dentalclinicmanagement.service.CalendarService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/calendar")
@RequiredArgsConstructor
@Slf4j
public class GoogleCalendarController {

    private final GoogleCredentialRepository credentialRepository;
    private final AppointmentCalendarLinkRepository linkRepository;
    private final UserRepository userRepository;
    private final CalendarService calendarService;

    @Value("${app.backend.url:http://localhost:8080}")
    private String backendUrl;

    @GetMapping("/link-url")
    public ResponseEntity<Map<String, Object>> getCalendarLinkUrl(@AuthenticationPrincipal UserDetails userDetails) {
        Map<String, Object> response = new HashMap<>();

        try {
            User user = userRepository.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Optional<GoogleCredential> existingCred = credentialRepository.findByUser(user);
            if (existingCred.isPresent() && calendarService.hasCalendarAccess(user)) {
                response.put("status", "ALREADY_LINKED");
                response.put("message", "Google Calendar is already connected");
                return ResponseEntity.ok(response);
            }

            String encodedEmail = URLEncoder.encode(user.getEmail(), StandardCharsets.UTF_8);
            String authUrl = backendUrl + "/oauth2/authorization/google" +
                    "?link_mode=calendar&existing_user=" + encodedEmail;

            response.put("status", "SUCCESS");
            response.put("authUrl", authUrl);
            response.put("message", "Open this URL to connect Google Calendar to your existing account");
            response.put("userEmail", user.getEmail());

            return ResponseEntity.ok(response);

        } catch (Exception ex) {
            log.error("Error generating calendar link URL", ex);
            response.put("status", "ERROR");
            response.put("message", ex.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getCalendarStatus(@AuthenticationPrincipal UserDetails userDetails) {
        Map<String, Object> response = new HashMap<>();

        try {
            User user = userRepository.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Optional<GoogleCredential> credOpt = credentialRepository.findByUser(user);

            if (credOpt.isEmpty()) {
                response.put("linked", false);
                response.put("message", "Google Calendar not connected");
                return ResponseEntity.ok(response);
            }

            boolean hasAccess = calendarService.hasCalendarAccess(user);
            response.put("linked", hasAccess);
            response.put("message", hasAccess ?
                    "Google Calendar connected" :
                    "Calendar credentials invalid, please re-authorize");

            return ResponseEntity.ok(response);

        } catch (Exception ex) {
            log.error("Error checking calendar status", ex);
            response.put("status", "ERROR");
            response.put("message", ex.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @DeleteMapping("/unlink")
    @Transactional
    public ResponseEntity<MessageResponse> unlinkCalendar(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            User user = userRepository.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            credentialRepository.findByUser(user).ifPresent(credentialRepository::delete);
            linkRepository.deleteAllByUserId(user.getId());

            return ResponseEntity.ok(new MessageResponse("Calendar disconnected successfully"));

        } catch (Exception ex) {
            log.error("Error unlinking calendar", ex);
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Failed to disconnect calendar: " + ex.getMessage()));
        }
    }
}