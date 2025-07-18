package org.example.dentalclinicmanagement.security.oauth2;

import org.example.dentalclinicmanagement.dto.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.dentalclinicmanagement.exception.handler.GlobalExceptionHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    @Value("${app.frontend.url}")
    private String frontendUrl;

    private final GlobalExceptionHandler globalExceptionHandler;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {

        log.error("OAuth2 authentication failed: {}", exception.getMessage());

        ResponseEntity<ErrorResponse> errorResponse;

        if (exception instanceof DisabledException) {
            errorResponse = globalExceptionHandler.handleDisabledException((DisabledException) exception);
        } else {
            errorResponse = globalExceptionHandler.handleAuthenticationException(exception);
        }

        String errorMessage = "Authentication failed";
        if (errorResponse != null && errorResponse.getBody() != null) {
            errorMessage = errorResponse.getBody().getMessage();
        }

        String targetUrl = UriComponentsBuilder.fromUriString(frontendUrl + "/login")
                .queryParam("error", "authentication_failed")
                .queryParam("message", URLEncoder.encode(errorMessage, StandardCharsets.UTF_8))
                .build().toUriString();

        log.info("Redirecting OAuth2 failure to: {} with message: {}", targetUrl, errorMessage);
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}