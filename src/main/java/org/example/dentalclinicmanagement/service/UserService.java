package org.example.dentalclinicmanagement.service;

import org.example.dentalclinicmanagement.dto.DentistProfileDto;
import org.example.dentalclinicmanagement.dto.UserDTO;
import org.example.dentalclinicmanagement.dto.request.ChangePasswordRequest;
import org.example.dentalclinicmanagement.dto.request.ForgotPasswordRequest;
import org.example.dentalclinicmanagement.dto.request.ResetPasswordRequest;
import org.example.dentalclinicmanagement.dto.request.UpdateUserProfileRequest;
import org.example.dentalclinicmanagement.dto.response.MessageResponse;
import org.example.dentalclinicmanagement.model.Role;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;

public interface UserService {

    UserDTO getUserByEmail(String email);

    UserDTO getUserById(Long id);

    Page<UserDTO> getAllUsersWithFilters(int page, int size, Role role, String search);

    UserDTO updateUserRole(Long userId, Role role);

    UserDTO updateUserProfile(Long id, UpdateUserProfileRequest request);

    MessageResponse changePassword(Long id, ChangePasswordRequest request);

    void generatePasswordResetToken(ForgotPasswordRequest request);

    void resetPassword(ResetPasswordRequest request);

    MessageResponse updatePriorityDentist(Long userId, Long dentistId);

    DentistProfileDto getPriorityDentistProfile(Long userId);

}