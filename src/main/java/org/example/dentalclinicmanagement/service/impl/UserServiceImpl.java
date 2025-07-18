package org.example.dentalclinicmanagement.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.dentalclinicmanagement.dto.DentistProfileDto;
import org.example.dentalclinicmanagement.dto.UserDto;
import org.example.dentalclinicmanagement.dto.request.ChangePasswordRequest;
import org.example.dentalclinicmanagement.dto.request.ForgotPasswordRequest;
import org.example.dentalclinicmanagement.dto.request.ResetPasswordRequest;
import org.example.dentalclinicmanagement.dto.request.UpdateUserProfileRequest;
import org.example.dentalclinicmanagement.dto.response.MessageResponse;
import org.example.dentalclinicmanagement.exception.PasswordUpdateException;
import org.example.dentalclinicmanagement.exception.ResetPasswordException;
import org.example.dentalclinicmanagement.exception.UserNotFoundException;
import org.example.dentalclinicmanagement.mapper.UserMapper;
import org.example.dentalclinicmanagement.model.DentistProfile;
import org.example.dentalclinicmanagement.model.PasswordResetToken;
import org.example.dentalclinicmanagement.model.Role;
import org.example.dentalclinicmanagement.model.User;
import org.example.dentalclinicmanagement.repository.DentistProfileRepository;
import org.example.dentalclinicmanagement.repository.PasswordResetTokenRepository;
import org.example.dentalclinicmanagement.repository.UserRepository;
import org.example.dentalclinicmanagement.service.UserService;
import org.example.dentalclinicmanagement.util.EmailUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final DentistProfileRepository dentistProfileRepository;
    private final EmailUtil emailUtils;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Override
    @Transactional(readOnly = true)
    public UserDto getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + email));

        return userMapper.toUserDTO(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserDto getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + id));

        return userMapper.toUserDTO(user);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserDto> getAllUsersWithFilters(int page, int size, Role role, String search) {
        log.debug("Getting users with search: '{}' and filters: role={}, page={}, size={}",
                search, role, page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by("email").ascending());

        Page<User> users = userRepository.findUsersWithFilters(search, role, pageable);

        return users.map(userMapper::toUserDTO);
    }

    @Override
    public UserDto updateUserRole(Long userId, Role role) {
        log.debug("Updating user role: userId={}, newRole={}", userId, role);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));

        Role oldRole = user.getRole();
        int totalAdmins = userRepository.countByRole(Role.ADMIN);
        if (oldRole.equals(Role.ADMIN) && totalAdmins <= 1) {
            throw new IllegalArgumentException("Cannot change role of an admin user");
        }
        user.setRole(role);
        User updatedUser = userRepository.save(user);

        log.info("User role updated: userId={}, role={}->{}", userId, oldRole, role);
        return userMapper.toUserDTO(updatedUser);
    }


    @Override
    @Transactional
    public UserDto updateUserProfile(Long userId, UpdateUserProfileRequest request) {
        log.debug("Updating user profile for id: {}", userId);

        User existingUser = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));

        if (request.getFirstName() != null && !request.getFirstName().trim().isEmpty()) {
            existingUser.setFirstName(request.getFirstName().trim());
        }
        if (request.getLastName() != null && !request.getLastName().trim().isEmpty()) {
            existingUser.setLastName(request.getLastName().trim());
        }
        if (request.getPhoneNumber() != null) {
            existingUser.setPhoneNumber(request.getPhoneNumber().trim());
        }

        User updatedUser = userRepository.save(existingUser);
        log.info("User profile updated: userId={}, email={}", userId, updatedUser.getEmail());
        return userMapper.toUserDTO(updatedUser);
    }

    @Override
    public MessageResponse changePassword(Long userId, ChangePasswordRequest request) {
        log.debug("Attempting to change password for user: {}", userId);

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new PasswordUpdateException("update.passwords.do.not.match");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            log.warn("Incorrect current password for user: {}", userId);
            throw new PasswordUpdateException("update.current.password.incorrect");
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            log.warn("New password same as current for user: {}", userId);
            throw new PasswordUpdateException("update.new.password.must.be.different");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        log.info("Password successfully changed for user: {}", userId);
        return new MessageResponse("Password changed successfully");
    }

    @Override
    @Transactional
    public void generatePasswordResetToken(ForgotPasswordRequest request) {
        log.debug("Generating password reset token for email: {}", request.getEmail());

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        String token = UUID.randomUUID().toString();
        Instant expiryDate = Instant.now().plus(24, ChronoUnit.HOURS);
        PasswordResetToken resetToken = new PasswordResetToken(token, expiryDate, user);
        passwordResetTokenRepository.save(resetToken);

        String resetUrl = frontendUrl + "/reset-password?token=" + token;
        emailUtils.sendPasswordResetEmail(user.getEmail(), resetUrl);

        log.info("Password reset email sent to: {}", user.getEmail());
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        log.debug("Processing password reset for token: {}", request.getToken().substring(0, 8) + "...");

        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(request.getToken())
                .orElseThrow(() -> new ResetPasswordException("reset.password.invalid"));

        if (resetToken.isUsed()) {
            log.warn("Attempt to use already used reset token");
            throw new ResetPasswordException("reset.password.token.already.used");
        }
        if (resetToken.getExpiryDate().isBefore(Instant.now())) {
            log.warn("Attempt to use expired reset token");
            throw new ResetPasswordException("reset.password.token.expired");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);
        userRepository.save(user);

        log.info("Password successfully reset for user: {}", user.getEmail());
    }

    @Override
    public MessageResponse updatePriorityDentist(Long userId, Long dentistId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        DentistProfile dentist = dentistProfileRepository.findById(dentistId)
                .orElseThrow(() -> new UserNotFoundException("Dentist not found"));

        User dentistProfile = userRepository.findById(dentist.getDentist().getId()).orElseThrow(() -> new UserNotFoundException("Dentist not found"));
        if (!dentistProfile.getRole().equals(Role.DENTIST)) {
            throw new IllegalArgumentException("Selected user is not a dentist");
        }
        user.setPriorityDentist(dentistProfile);
        userRepository.save(user);
        return new MessageResponse("Priority dentist updated successfully!");
    }


    @Override
    public DentistProfileDto getPriorityDentistProfile(Long userId) {
        User currentUser = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        User priorityDentist = currentUser.getPriorityDentist();
        if (priorityDentist == null) {
            return null;
        }

        DentistProfile profile = dentistProfileRepository.findByDentist(priorityDentist)
                .orElseThrow(() -> new IllegalStateException("Priority dentist profile not found"));

        return DentistProfileDto.builder()
                .dentistId(priorityDentist.getId())
               .description(profile.getDescription())
                .experience(profile.getYearsOfExperience())
                .firstName(priorityDentist.getFirstName())
                .lastName(priorityDentist.getLastName())
                .photoUrl(profile.getPhotoUrl())
                .build();
    }
}