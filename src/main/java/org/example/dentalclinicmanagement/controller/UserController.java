package org.example.dentalclinicmanagement.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.dentalclinicmanagement.dto.DentistProfileDto;
import org.example.dentalclinicmanagement.dto.UserDto;
import org.example.dentalclinicmanagement.dto.request.ChangePasswordRequest;
import org.example.dentalclinicmanagement.dto.request.UpdateUserProfileRequest;
import org.example.dentalclinicmanagement.dto.response.MessageResponse;
import org.example.dentalclinicmanagement.model.Role;
import org.example.dentalclinicmanagement.security.service.UserDetailsImpl;
import org.example.dentalclinicmanagement.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/users")
@AllArgsConstructor
@Validated
public class UserController {

    private final UserService userService;

    @GetMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserDto> getCurrentUserInfo() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) auth.getPrincipal();

        log.debug("Profile info request from user: {}", userDetails.getEmail());

        UserDto userDTO = userService.getUserByEmail(userDetails.getEmail());
        return ResponseEntity.ok(userDTO);
    }

    @PatchMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserDto> updateCurrentUserInfo(
            @Valid @RequestBody UpdateUserProfileRequest request) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) auth.getPrincipal();

        log.info("User profile update request: user={}", userDetails.getEmail());

        UserDto updatedUser = userService.updateUserProfile(userDetails.getId(), request);
        return ResponseEntity.ok(updatedUser);
    }

    @PatchMapping("/priority-dentist")
    public ResponseEntity<MessageResponse> updatePriorityDentist(@RequestParam Long dentistId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) auth.getPrincipal();

        log.info("User priority dentist update request: user={}", userDetails.getEmail());

        MessageResponse response = userService.updatePriorityDentist(userDetails.getId(), dentistId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/priority-dentist")
    public ResponseEntity<?> getPriorityDentist() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) auth.getPrincipal();

        DentistProfileDto dentistProfileDto = userService.getPriorityDentistProfile(userDetails.getId());

        if (dentistProfileDto == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(dentistProfileDto);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<UserDto>> getAllUsers(
            @RequestParam(name = "page", defaultValue = "0") @Min(0) int page,
            @RequestParam(name = "size", defaultValue = "10") @Min(1) int size,
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "role", required = false) Role role) {

        log.debug("Admin users listing request: page={}, size={}, role={}, search='{}'", page, size, role, search);

        return ResponseEntity.ok(userService.getAllUsersWithFilters(page, size, role, search));
    }

    @PatchMapping("/profile/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDto> updateUser(
            @Valid @RequestBody UpdateUserProfileRequest request,
            @PathVariable Long id) {

        log.info("Admin user update request: userId={}", id);

        UserDto updatedUser = userService.updateUserProfile(id, request);
        return ResponseEntity.ok(updatedUser);
    }

    @GetMapping("/profile/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DENTIST', 'MANAGER')")
    public ResponseEntity<UserDto> getUserById(@PathVariable Long id) {
        log.info("Getting info about user: userId={}", id);

        return ResponseEntity.ok(userService.getUserById(id));
    }

    @PatchMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDto> updateUserRole(@PathVariable Long id, @RequestParam @NotNull Role role) {
        log.info("Admin role update request: userId={}, newRole={}", id, role);

        UserDto updatedUser = userService.updateUserRole(id, role);
        return ResponseEntity.ok(updatedUser);
    }

    @PatchMapping("/profile/change-password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MessageResponse> changePassword(
            @Valid @RequestBody ChangePasswordRequest request) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) auth.getPrincipal();

        log.info("Password change request from user: {}", userDetails.getEmail());
        log.debug("Password change request details: userId={}, request={}", userDetails.getId(), request);
        MessageResponse response = userService.changePassword(userDetails.getId(), request);
        return ResponseEntity.ok(response);
    }
}