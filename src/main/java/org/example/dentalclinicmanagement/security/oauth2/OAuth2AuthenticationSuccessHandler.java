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

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        String targetUrl = determineTargetUrl(request, response, authentication);

        if (response.isCommitted()) {
            log.debug("Response has already been committed. Unable to redirect to {}", targetUrl);
            return;
        }

        clearAuthenticationAttributes(request);
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    @Transactional
    protected String determineTargetUrl(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) {

        Object principal = authentication.getPrincipal();
        UserDetailsImpl userDetails;

        if (principal instanceof UserDetailsImpl) {
            userDetails = (UserDetailsImpl) principal;
            log.info("Regular login user: {}", userDetails.getEmail());

            if (!userDetails.isEnabled()) {
                log.warn("Login attempt for disabled user: {}", userDetails.getEmail());
                throw new DisabledException("User account is disabled");
            }

        } else if (principal instanceof OAuth2User oAuth2User) {
            String email = oAuth2User.getAttribute("email");
            String firstName = oAuth2User.getAttribute("given_name");
            String lastName = oAuth2User.getAttribute("family_name");

            if (email == null) {
                throw new IllegalStateException("Email not found in OAuth2 user attributes");
            }

            log.info("Processing OAuth2 user: {}", email);

            User user = userRepository.findByEmail(email).orElseGet(() -> {
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
            });

            boolean userUpdated = false;
            if (firstName != null && !firstName.equals(user.getFirstName())) {
                user.setFirstName(firstName);
                userUpdated = true;
            }
            if (lastName != null && !lastName.equals(user.getLastName())) {
                user.setLastName(lastName);
                userUpdated = true;
            }

            if (userUpdated) {
                user = userRepository.save(user);
                log.info("OAuth2 user data updated: {} (ID: {})", user.getEmail(), user.getId());
            }

            storeCalendarCredentials(authentication, user);

            userDetails = UserDetailsImpl.build(user, oAuth2User.getAttributes());
            log.info("OAuth2 user processed: {} (ID: {})", user.getEmail(), user.getId());

        } else {
            log.error("Unexpected principal type: {}", principal.getClass());
            throw new IllegalStateException("Unexpected authentication principal type");
        }

        try {
            String jwt = jwtUtils.generateJwtToken(userDetails);
            log.info("Tokens generated successfully for user: {} (ID: {})",
                    userDetails.getEmail(), userDetails.getId());

            return UriComponentsBuilder.fromUriString(frontendUrl + "/oauth2/redirect")
                    .queryParam("token", URLEncoder.encode(jwt, StandardCharsets.UTF_8))
                    .build().toUriString();

        } catch (Exception e) {
            log.error("Error generating tokens for user: {}", userDetails.getEmail(), e);
            throw new RuntimeException("Failed to generate authentication tokens", e);
        }
    }

    private void storeCalendarCredentials(Authentication authentication, User user) {
        try {
            OAuth2AuthorizedClient authorizedClient = authorizedClientService
                    .loadAuthorizedClient("google", authentication.getName());

            if (authorizedClient != null && authorizedClient.getAccessToken() != null) {
                String accessToken = authorizedClient.getAccessToken().getTokenValue();
                String refreshToken = authorizedClient.getRefreshToken() != null ?
                        authorizedClient.getRefreshToken().getTokenValue() : null;
                Long expiresAt = authorizedClient.getAccessToken().getExpiresAt() != null ?
                        authorizedClient.getAccessToken().getExpiresAt().toEpochMilli() : null;

                if (refreshToken != null && expiresAt != null) {
                    calendarService.storeCredentials(user, accessToken, refreshToken, expiresAt);
                    log.info("Google Calendar credentials stored for user: {}", user.getEmail());
                } else {
                    log.warn("Missing refresh token or expiration for user: {}", user.getEmail());
                }
            }
        } catch (Exception ex) {
            log.warn("Failed to store calendar credentials for user {}: {}", user.getEmail(), ex.getMessage());
        }
    }
}