package org.example.dentalclinicmanagement.service;

import org.example.dentalclinicmanagement.dto.request.LoginRequest;
import org.example.dentalclinicmanagement.dto.request.SignupRequest;
import org.example.dentalclinicmanagement.dto.response.JwtResponse;

public interface AuthService {

    JwtResponse authenticateUser(LoginRequest loginRequest);

    JwtResponse registerAndLogin(SignupRequest signUpRequest);
}
