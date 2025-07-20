package org.example.dentalclinicmanagement.service.impl;

import org.example.dentalclinicmanagement.dto.request.LoginRequest;
import org.example.dentalclinicmanagement.dto.request.SignupRequest;
import org.example.dentalclinicmanagement.dto.response.JwtResponse;
import org.example.dentalclinicmanagement.exception.EmailAlreadyExistsException;
import org.example.dentalclinicmanagement.model.Role;
import org.example.dentalclinicmanagement.model.User;
import org.example.dentalclinicmanagement.repository.UserRepository;
import org.example.dentalclinicmanagement.security.jwt.JwtUtils;
import org.example.dentalclinicmanagement.security.service.UserDetailsImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private AuthServiceImpl authService;

    private LoginRequest loginRequest;
    private SignupRequest signupRequest;
    private UserDetailsImpl userDetails;
    private User user;

    @BeforeEach
    void setUp() {
        loginRequest = new LoginRequest("test@example.com", "password123");

        signupRequest = new SignupRequest();
        signupRequest.setFirstName("John");
        signupRequest.setLastName("Doe");
        signupRequest.setEmail("john.doe@example.com");
        signupRequest.setPassword("password123");
        signupRequest.setPhoneNumber("+1234567890");

        user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setRole(Role.USER);

        userDetails = UserDetailsImpl.build(user);
    }

    @Test
    void authenticateUser_Success() {
        String expectedJwt = "jwt-token";
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(jwtUtils.generateJwtToken(userDetails)).thenReturn(expectedJwt);

        JwtResponse response = authService.authenticateUser(loginRequest);

        assertNotNull(response);
        assertEquals(expectedJwt, response.getToken());
        assertEquals(userDetails.getId(), response.getId());
        assertEquals(userDetails.getEmail(), response.getEmail());
        assertEquals(List.of("ROLE_USER"), response.getRoles());

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtUtils).generateJwtToken(userDetails);
    }

    @Test
    void authenticateUser_LowerCaseEmail() {
        LoginRequest upperCaseEmailRequest = new LoginRequest("TEST@EXAMPLE.COM", "password123");
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(jwtUtils.generateJwtToken(userDetails)).thenReturn("jwt-token");

        authService.authenticateUser(upperCaseEmailRequest);

        verify(authenticationManager).authenticate(
                argThat(auth -> auth.getPrincipal().equals("test@example.com"))
        );
    }

    @Test
    void registerAndLogin_Success() {
        when(userRepository.existsByEmail(signupRequest.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(signupRequest.getPassword())).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(jwtUtils.generateJwtToken(userDetails)).thenReturn("jwt-token");

        JwtResponse response = authService.registerAndLogin(signupRequest);

        assertNotNull(response);
        assertEquals("jwt-token", response.getToken());
        assertEquals(userDetails.getId(), response.getId());
        assertEquals(userDetails.getEmail(), response.getEmail());

        verify(userRepository).existsByEmail(signupRequest.getEmail());
        verify(passwordEncoder).encode(signupRequest.getPassword());
        verify(userRepository).save(argThat(savedUser ->
                savedUser.getFirstName().equals("John") &&
                        savedUser.getLastName().equals("Doe") &&
                        savedUser.getEmail().equals("john.doe@example.com") &&
                        savedUser.getPhoneNumber().equals("+1234567890") &&
                        savedUser.getRole().equals(Role.USER)
        ));
    }

    @Test
    void registerAndLogin_EmailAlreadyExists() {
        when(userRepository.existsByEmail(signupRequest.getEmail())).thenReturn(true);

        EmailAlreadyExistsException exception = assertThrows(
                EmailAlreadyExistsException.class,
                () -> authService.registerAndLogin(signupRequest)
        );

        assertEquals("User with email john.doe@example.com already exists", exception.getMessage());
        verify(userRepository).existsByEmail(signupRequest.getEmail());
        verify(userRepository, never()).save(any(User.class));
        verify(authenticationManager, never()).authenticate(any());
    }

    @Test
    void registerAndLogin_CreatesUserWithCorrectDefaultRole() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(jwtUtils.generateJwtToken(any())).thenReturn("jwt-token");

        authService.registerAndLogin(signupRequest);

        verify(userRepository).save(argThat(savedUser ->
                savedUser.getRole().equals(Role.USER)
        ));
    }

    @Test
    void registerAndLogin_EncodesPassword() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(jwtUtils.generateJwtToken(any())).thenReturn("jwt-token");

        authService.registerAndLogin(signupRequest);

        verify(passwordEncoder).encode("password123");
        verify(userRepository).save(argThat(savedUser ->
                savedUser.getPassword().equals("encoded-password")
        ));
    }
}