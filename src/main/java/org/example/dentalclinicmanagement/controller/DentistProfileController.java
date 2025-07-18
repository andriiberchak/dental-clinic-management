package org.example.dentalclinicmanagement.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.dentalclinicmanagement.dto.DentistProfileDto;
import org.example.dentalclinicmanagement.dto.request.UpdateDentistProfileRequest;
import org.example.dentalclinicmanagement.dto.response.MessageResponse;
import org.example.dentalclinicmanagement.security.service.UserDetailsImpl;
import org.example.dentalclinicmanagement.service.DentistProfileService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/api/dentist-profile")
@RequiredArgsConstructor
public class DentistProfileController {

    private final DentistProfileService dentistProfileService;

    @GetMapping("/{dentistId}")
    public ResponseEntity<DentistProfileDto> getDentistProfile(@PathVariable Long dentistId) {
        log.debug("Getting dentist profile for dentistId: {}", dentistId);
        DentistProfileDto profile = dentistProfileService.getDentistProfile(dentistId);
        return ResponseEntity.ok(profile);
    }

    @GetMapping("/all")
    public ResponseEntity<Page<DentistProfileDto>> getAllDentists(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.debug("Getting all dentist profiles: page={}, size={}", page, size);
        Page<DentistProfileDto> profiles = dentistProfileService.getAllDentistProfiles(page, size);
        return ResponseEntity.ok(profiles);
    }

    @PostMapping("/profile")
    @PreAuthorize("hasRole('DENTIST')")
    public ResponseEntity<DentistProfileDto> updateOwnProfile(
            @Valid @RequestBody UpdateDentistProfileRequest request) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) auth.getPrincipal();

        log.info("Dentist profile update request from user: {}", userDetails.getEmail());

        DentistProfileDto updated = dentistProfileService.updateDentistProfile(
                userDetails.getId(), request);
        return ResponseEntity.ok(updated);
    }

    @PutMapping("/{dentistId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DentistProfileDto> updateDentistProfile(
            @PathVariable Long dentistId,
            @Valid @RequestBody UpdateDentistProfileRequest request) {

        log.info("Admin updating dentist profile: dentistId={}", dentistId);

        DentistProfileDto updated = dentistProfileService.updateDentistProfile(dentistId, request);
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/{dentistId}/photo")
    @PreAuthorize("hasRole('DENTIST') or hasRole('ADMIN')")
    public ResponseEntity<MessageResponse> uploadPhoto(
            @PathVariable Long dentistId,
            @RequestParam("file") MultipartFile file) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) auth.getPrincipal();

        boolean isAdmin = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        boolean isOwnProfile = userDetails.getId().equals(dentistId);

        if (!isAdmin && !isOwnProfile) {
            return ResponseEntity.status(403)
                    .body(new MessageResponse("Access denied: can only update own profile"));
        }

        log.info("Photo upload request for dentist: {}", dentistId);

        String photoUrl = dentistProfileService.uploadPhoto(dentistId, file);
        return ResponseEntity.ok(new MessageResponse("Photo uploaded successfully: " + photoUrl));
    }

    @DeleteMapping("/{dentistId}/photo")
    @PreAuthorize("hasRole('DENTIST') or hasRole('ADMIN')")
    public ResponseEntity<MessageResponse> deletePhoto(@PathVariable Long dentistId) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) auth.getPrincipal();

        boolean isAdmin = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        boolean isOwnProfile = userDetails.getId().equals(dentistId);

        if (!isAdmin && !isOwnProfile) {
            return ResponseEntity.status(403)
                    .body(new MessageResponse("Access denied"));
        }

        dentistProfileService.deletePhoto(dentistId);
        return ResponseEntity.ok(new MessageResponse("Photo deleted successfully"));
    }
}