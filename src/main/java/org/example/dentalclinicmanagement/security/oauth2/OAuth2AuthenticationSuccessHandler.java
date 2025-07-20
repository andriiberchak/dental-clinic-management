package org.example.dentalclinicmanagement.security.oauth2;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.dentalclinicmanagement.model.Role;
import org.example.dentalclinicmanagement.model.User;
import org.example.dentalclinicmanagement.repository.UserRepository;
import org.example.dentalclinicmanagement.security.jwt.JwtUtils;
import org.example.dentalclinicmanagement.security.service.UserDetailsImpl;
import org.example.dentalclinicmanagement.service.CalendarService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtUtils jwtUtils;
    private final UserRepository userRepository;
    private final CalendarService calendarService;
    private final OAuth2AuthorizedClientService authorizedClientService;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    private static final String CALENDAR_STATE_DELIMITER = "|calendar|";
    private static final String OAUTH2_REDIRECT_PATH = "/oauth2/redirect";
    private static final String CALENDAR_SUCCESS_PATH = "/calendar/link-success";
    private static final String CALENDAR_ERROR_PATH = "/calendar/link-error";

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        String state = request.getParameter("state");
        log.info("OAuth2 authentication success - state: {}", state);

        CalendarLinkInfo linkInfo = parseCalendarLinkState(state);

        if (linkInfo != null) {
            log.info("Calendar linking mode detected for user: {}", linkInfo.existingUserEmail());
            handleCalendarLinking(request, response, authentication, linkInfo.existingUserEmail());
            return;
        }

        log.info("Regular OAuth2 login mode");
        handleRegularLogin(request, response, authentication);
    }

    private void handleRegularLogin(HttpServletRequest request, HttpServletResponse response,
                                    Authentication authentication) throws IOException {
        String targetUrl = determineTargetUrl(authentication);

        if (response.isCommitted()) {
            log.debug("Response already committed, cannot redirect to: {}", targetUrl);
            return;
        }

        clearAuthenticationAttributes(request);
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    @Transactional
    protected void handleCalendarLinking(HttpServletRequest request, HttpServletResponse response,
                                         Authentication authentication, String existingUserEmail) throws IOException {
        try {
            log.info("Processing calendar linking for user: {}", existingUserEmail);

            User existingUser = userRepository.findByEmail(existingUserEmail)
                    .orElseThrow(() -> new RuntimeException("User not found: " + existingUserEmail));

            boolean success = storeCalendarCredentials(authentication, existingUser);
            String redirectUrl = buildCalendarRedirectUrl(success, existingUserEmail);

            log.info("Calendar linking {} for user: {}", success ? "successful" : "failed", existingUserEmail);
            getRedirectStrategy().sendRedirect(request, response, redirectUrl);

        } catch (Exception ex) {
            log.error("Calendar linking error for user {}: {}", existingUserEmail, ex.getMessage());
            String errorUrl = buildCalendarErrorUrl(ex.getMessage());
            getRedirectStrategy().sendRedirect(request, response, errorUrl);
        }
    }

    @Transactional
    protected String determineTargetUrl(Authentication authentication) {
        UserDetailsImpl userDetails = processOAuth2User(authentication);

        try {
            String jwt = jwtUtils.generateJwtToken(userDetails);
            log.info("JWT generated for user: {} (ID: {})", userDetails.getEmail(), userDetails.getId());

            return UriComponentsBuilder.fromUriString(frontendUrl + OAUTH2_REDIRECT_PATH)
                    .queryParam("token", URLEncoder.encode(jwt, StandardCharsets.UTF_8))
                    .build().toUriString();

        } catch (Exception e) {
            log.error("JWT generation failed for user: {}", userDetails.getEmail(), e);
            throw new RuntimeException("Failed to generate authentication tokens", e);
        }
    }

    private UserDetailsImpl processOAuth2User(Authentication authentication) {
        Object principal = authentication.getPrincipal();

        if (principal instanceof UserDetailsImpl userDetails) {
            validateUserAccount(userDetails);
            return userDetails;
        }

        if (principal instanceof OAuth2User oAuth2User) {
            return handleOAuth2User(oAuth2User, authentication);
        }

        throw new IllegalStateException("Unexpected principal type: " + principal.getClass());
    }

    private UserDetailsImpl handleOAuth2User(OAuth2User oAuth2User, Authentication authentication) {
        String email = oAuth2User.getAttribute("email");
        String firstName = oAuth2User.getAttribute("given_name");
        String lastName = oAuth2User.getAttribute("family_name");

        if (email == null) {
            throw new IllegalStateException("Email not found in OAuth2 user attributes");
        }

        log.info("Processing OAuth2 user: {}", email);

        User user = userRepository.findByEmail(email)
                .map(existingUser -> updateUserIfNeeded(existingUser, firstName, lastName))
                .orElseGet(() -> createNewOAuth2User(email, firstName, lastName));

        storeCalendarCredentials(authentication, user);

        UserDetailsImpl userDetails = UserDetailsImpl.build(user, oAuth2User.getAttributes());
        log.info("OAuth2 user processed: {} (ID: {})", user.getEmail(), user.getId());

        return userDetails;
    }

    private User createNewOAuth2User(String email, String firstName, String lastName) {
        log.info("Creating new OAuth2 user: {}", email);

        User newUser = new User();
        newUser.setFirstName(firstName != null ? firstName : "");
        newUser.setLastName(lastName != null ? lastName : "");
        newUser.setEmail(email);
        newUser.setPassword("");
        newUser.setRole(Role.USER);

        User savedUser = userRepository.save(newUser);
        log.info("New OAuth2 user created with ID: {}", savedUser.getId());
        return savedUser;
    }

    private User updateUserIfNeeded(User user, String firstName, String lastName) {
        boolean updated = false;

        if (firstName != null && !firstName.equals(user.getFirstName())) {
            user.setFirstName(firstName);
            updated = true;
        }
        if (lastName != null && !lastName.equals(user.getLastName())) {
            user.setLastName(lastName);
            updated = true;
        }

        if (updated) {
            user = userRepository.save(user);
            log.info("OAuth2 user data updated: {} (ID: {})", user.getEmail(), user.getId());
        }

        return user;
    }

    private void validateUserAccount(UserDetailsImpl userDetails) {
        log.info("Regular login user: {}", userDetails.getEmail());

        if (!userDetails.isEnabled()) {
            log.warn("Login attempt for disabled user: {}", userDetails.getEmail());
            throw new DisabledException("User account is disabled");
        }
    }

    private boolean storeCalendarCredentials(Authentication authentication, User user) {
        try {
            OAuth2AuthorizedClient authorizedClient = authorizedClientService
                    .loadAuthorizedClient("google", authentication.getName());

            if (authorizedClient == null || authorizedClient.getAccessToken() == null) {
                log.warn("No OAuth2 authorized client or access token for user: {}", user.getEmail());
                return false;
            }

            String accessToken = authorizedClient.getAccessToken().getTokenValue();
            String refreshToken = authorizedClient.getRefreshToken() != null ?
                    authorizedClient.getRefreshToken().getTokenValue() : null;
            Long expiresAt = authorizedClient.getAccessToken().getExpiresAt() != null ?
                    authorizedClient.getAccessToken().getExpiresAt().toEpochMilli() : null;

            if (refreshToken != null && expiresAt != null) {
                calendarService.storeCredentials(user, accessToken, refreshToken, expiresAt);
                log.info("Google Calendar credentials stored for user: {}", user.getEmail());
                return true;
            } else {
                log.warn("Missing refresh token or expiration for user: {}", user.getEmail());
                return false;
            }

        } catch (Exception ex) {
            log.error("Failed to store calendar credentials for user {}: {}", user.getEmail(), ex.getMessage());
            return false;
        }
    }

    private CalendarLinkInfo parseCalendarLinkState(String state) {
        if (state == null || !state.contains(CALENDAR_STATE_DELIMITER)) {
            return null;
        }

        try {
            String[] parts = state.split("\\|");
            if (parts.length >= 3 && "calendar".equals(parts[1])) {
                return new CalendarLinkInfo(parts[2]);
            }
        } catch (Exception ex) {
            log.warn("Failed to parse calendar link state: {}", state, ex);
        }

        return null;
    }

    private String buildCalendarRedirectUrl(boolean success, String userEmail) {
        String path = success ? CALENDAR_SUCCESS_PATH : CALENDAR_ERROR_PATH;
        String paramName = success ? "email" : "error";
        String paramValue = success ? userEmail : "Failed to store calendar credentials";

        return UriComponentsBuilder.fromUriString(frontendUrl + path)
                .queryParam(paramName, URLEncoder.encode(paramValue, StandardCharsets.UTF_8))
                .build().toUriString();
    }

    private String buildCalendarErrorUrl(String errorMessage) {
        return UriComponentsBuilder.fromUriString(frontendUrl + CALENDAR_ERROR_PATH)
                .queryParam("error", URLEncoder.encode(errorMessage, StandardCharsets.UTF_8))
                .build().toUriString();
    }

    private record CalendarLinkInfo(String existingUserEmail) {
    }
}