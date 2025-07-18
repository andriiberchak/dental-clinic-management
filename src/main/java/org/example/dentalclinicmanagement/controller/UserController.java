package org.example.dentalclinicmanagement.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.dentalclinicmanagement.dto.DentistProfileDto;
import org.example.dentalclinicmanagement.dto.UserDTO;
import org.example.dentalclinicmanagement.dto.request.ChangePasswordRequest;
import org.example.dentalclinicmanagement.dto.request.UpdateUserProfileRequest;
import org.example.dentalclinicmanagement.dto.response.MessageResponse;
import org.example.dentalclinicmanagement.model.Role;
import org.example.dentalclinicmanagement.model.User;
import org.example.dentalclinicmanagement.security.service.UserDetailsImpl;
import org.example.dentalclinicmanagement.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;


@Slf4j
@RestController
@RequestMapping("/api/users")
@AllArgsConstructor
@Validated
public class UserController {

    private final UserService userService;

    @GetMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserDTO> getCurrentUserInfo() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) auth.getPrincipal();

        log.debug("Profile info request from user: {}", userDetails.getEmail());

        UserDTO userDTO = userService.getUserByEmail(userDetails.getEmail());
        return ResponseEntity.ok(userDTO);
    }

    @PatchMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserDTO> updateCurrentUserInfo(
            @Valid @RequestBody UpdateUserProfileRequest request) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) auth.getPrincipal();

        log.info("User profile update request: user={}", userDetails.getEmail());

        UserDTO updatedUser = userService.updateUserProfile(userDetails.getId(), request);
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


    /*************************************************************************************/

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<UserDTO>> getAllUsers(
            @RequestParam(name = "page", defaultValue = "0") @Min(0) int page,
            @RequestParam(name = "size", defaultValue = "10") @Min(1) int size,
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "role", required = false) Role role) {

        log.debug("Admin users listing request: page={}, size={}, role={}, search='{}'", page, size, role, search);

        return ResponseEntity.ok(userService.getAllUsersWithFilters(page, size, role, search));
    }

    @PatchMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDTO> updateUser(
            @Valid @RequestBody UpdateUserProfileRequest request,
            @RequestParam("userId") Long userId) {

        log.info("Admin user update request: userId={}", userId);

        UserDTO updatedUser = userService.updateUserProfile(userId, request);
        return ResponseEntity.ok(updatedUser);
    }

    @PatchMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDTO> updateUserRole(@PathVariable Long id, @RequestParam @NotNull Role role) {
        log.info("Admin role update request: userId={}, newRole={}", id, role);

        UserDTO updatedUser = userService.updateUserRole(id, role);
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