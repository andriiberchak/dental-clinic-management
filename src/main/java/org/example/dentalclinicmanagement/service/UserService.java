package org.example.dentalclinicmanagement.service;

import org.example.dentalclinicmanagement.dto.DentistProfileDto;
import org.example.dentalclinicmanagement.dto.MinimalUserRegistrationDTO;
import org.example.dentalclinicmanagement.dto.UserDto;
import org.example.dentalclinicmanagement.dto.request.ChangePasswordRequest;
import org.example.dentalclinicmanagement.dto.request.ForgotPasswordRequest;
import org.example.dentalclinicmanagement.dto.request.ResetPasswordRequest;
import org.example.dentalclinicmanagement.dto.request.UpdateUserProfileRequest;
import org.example.dentalclinicmanagement.dto.response.MessageResponse;
import org.example.dentalclinicmanagement.model.Role;
import org.springframework.data.domain.Page;

public interface UserService {

    UserDto getUserByEmail(String email);

    UserDto getUserById(Long id);

    Page<UserDto> getAllUsersWithFilters(int page, int size, Role role, String search);

    UserDto updateUserRole(Long userId, Role role);

    UserDto updateUserProfile(Long id, UpdateUserProfileRequest request);

    MessageResponse changePassword(Long id, ChangePasswordRequest request);

    void generatePasswordResetToken(ForgotPasswordRequest request);

    void resetPassword(ResetPasswordRequest request);

    MessageResponse updatePriorityDentist(Long userId, Long dentistId);

    DentistProfileDto getPriorityDentistProfile(Long userId);

    UserDto registerMinimalUser(MinimalUserRegistrationDTO dto);

}