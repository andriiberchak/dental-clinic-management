package org.example.dentalclinicmanagement.controller;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.dentalclinicmanagement.dto.request.ForgotPasswordRequest;
import org.example.dentalclinicmanagement.dto.request.LoginRequest;
import org.example.dentalclinicmanagement.dto.request.ResetPasswordRequest;
import org.example.dentalclinicmanagement.dto.request.SignupRequest;
import org.example.dentalclinicmanagement.dto.response.JwtResponse;
import org.example.dentalclinicmanagement.dto.response.MessageResponse;
import org.example.dentalclinicmanagement.service.AuthService;
import org.example.dentalclinicmanagement.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/auth")
@AllArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    @PostMapping("/sign-in")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        log.info("Login attempt for email: {}", loginRequest.getEmail());
        JwtResponse response = authService.authenticateUser(loginRequest);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/sign-up")
    public ResponseEntity<?> registerAndLoginUser(@Valid @RequestBody SignupRequest signUpRequest) {
        log.info("Registration attempt for email: {}", signUpRequest.getEmail());
        JwtResponse response = authService.registerAndLogin(signUpRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequest forgotPasswordRequest) {
        log.info("Password reset email sent for: {}", forgotPasswordRequest.getEmail());
        userService.generatePasswordResetToken(forgotPasswordRequest);
        return ResponseEntity.ok(new MessageResponse("Password reset email was sent"));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest resetPasswordRequest) {
        log.info("Password reset confirmation attempt for token: {}",
                resetPasswordRequest.getToken().substring(0, 8) + "...");
        userService.resetPassword(resetPasswordRequest);
        log.info("Password successfully reset for token: {}",
                resetPasswordRequest.getToken().substring(0, 8) + "...");
        return ResponseEntity.ok(new MessageResponse("Password reset successful"));
    }
}