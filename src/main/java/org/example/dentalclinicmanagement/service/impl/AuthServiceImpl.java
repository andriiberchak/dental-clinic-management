package org.example.dentalclinicmanagement.service.impl;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.dentalclinicmanagement.dto.request.LoginRequest;
import org.example.dentalclinicmanagement.dto.request.SignupRequest;
import org.example.dentalclinicmanagement.dto.response.JwtResponse;
import org.example.dentalclinicmanagement.exception.EmailAlreadyExistsException;
import org.example.dentalclinicmanagement.model.Role;
import org.example.dentalclinicmanagement.model.User;
import org.example.dentalclinicmanagement.repository.UserRepository;
import org.example.dentalclinicmanagement.security.jwt.JwtUtils;
import org.example.dentalclinicmanagement.security.service.UserDetailsImpl;
import org.example.dentalclinicmanagement.service.AuthService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@AllArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final JwtUtils jwtUtils;
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder encoder;

    @Override
    public JwtResponse authenticateUser(LoginRequest loginRequest) {
        log.info("Attempting authentication for email: {}", loginRequest.getEmail());

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getEmail().toLowerCase(), loginRequest.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        String jwt = jwtUtils.generateJwtToken(userDetails);
        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        log.info("User {} successfully logged in with roles: {}", userDetails.getEmail(), roles);

        return new JwtResponse(jwt, userDetails.getId(), userDetails.getEmail(), roles);
    }

    @Override
    @Transactional
    public JwtResponse registerAndLogin(SignupRequest signUpRequest) {
        log.debug("Starting user registration for email: {}", signUpRequest.getEmail());

        if (userRepository.existsByEmail(signUpRequest.getEmail())) {
            log.warn("Registration attempt failed - email already exists: {}", signUpRequest.getEmail());
            throw new EmailAlreadyExistsException("User with email " + signUpRequest.getEmail() + " already exists");
        }

        User user = new User();
        user.setFirstName(signUpRequest.getFirstName());
        user.setLastName(signUpRequest.getLastName());
        user.setEmail(signUpRequest.getEmail());
        user.setPassword(encoder.encode(signUpRequest.getPassword()));
        user.setPhoneNumber(signUpRequest.getPhoneNumber());
        user.setRole(Role.USER);

        userRepository.save(user);

        log.info("New user registered successfully: {} {} ({})",
                user.getFirstName(), user.getLastName(), user.getEmail());

        LoginRequest loginRequest = new LoginRequest(signUpRequest.getEmail(), signUpRequest.getPassword());
        return authenticateUser(loginRequest);
    }

}