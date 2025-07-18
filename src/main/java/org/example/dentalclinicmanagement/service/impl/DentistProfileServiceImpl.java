package org.example.dentalclinicmanagement.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.example.dentalclinicmanagement.dto.DentistProfileDto;
import org.example.dentalclinicmanagement.dto.request.UpdateDentistProfileRequest;
import org.example.dentalclinicmanagement.exception.FileStorageException;
import org.example.dentalclinicmanagement.exception.UserNotFoundException;
import org.example.dentalclinicmanagement.mapper.DentistProfileMapper;
import org.example.dentalclinicmanagement.model.DentistProfile;
import org.example.dentalclinicmanagement.model.Role;
import org.example.dentalclinicmanagement.model.User;
import org.example.dentalclinicmanagement.repository.DentistProfileRepository;
import org.example.dentalclinicmanagement.repository.UserRepository;
import org.example.dentalclinicmanagement.service.DentistProfileService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.Arrays;
import java.util.List;

@Service
@Slf4j
public class DentistProfileServiceImpl implements DentistProfileService {

    private final UserRepository userRepository;
    private final DentistProfileRepository dentistProfileRepository;
    private final DentistProfileMapper dentistProfileMapper;

    private final Path uploadDir;

    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList("jpg", "jpeg", "png", "gif");
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

    public DentistProfileServiceImpl(UserRepository userRepository,
                                     DentistProfileRepository dentistProfileRepository,
                                     DentistProfileMapper dentistProfileMapper,
                                     @Value("${file.upload-dir}") String uploadDir) {
        this.userRepository = userRepository;
        this.dentistProfileRepository = dentistProfileRepository;
        this.dentistProfileMapper = dentistProfileMapper;
        this.uploadDir = Paths.get(uploadDir).toAbsolutePath().normalize();

        try {
            Files.createDirectories(this.uploadDir);
        } catch (IOException e) {
            throw new FileStorageException("Could not create upload directory", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public DentistProfileDto getDentistProfile(Long dentistId) {
        log.debug("Getting dentist profile for dentistId: {}", dentistId);

        User dentist = userRepository.findById(dentistId)
                .orElseThrow(() -> new UserNotFoundException("Dentist not found with id: " + dentistId));

        if (!dentist.getRole().equals(Role.DENTIST)) {
            throw new IllegalArgumentException("User is not a dentist");
        }

        DentistProfile profile = dentistProfileRepository.findByDentist(dentist)
                .orElseGet(() -> createDefaultProfile(dentist));

        return dentistProfileMapper.toDto(profile);
    }

    @Transactional(readOnly = true)
    @Override
    public Page<DentistProfileDto> getAllDentistProfiles(int page, int size) {
        log.debug("Getting all dentist profiles: page={}, size={}", page, size);

        Pageable pageable = PageRequest.of(page, size,
                Sort.by("dentist.firstName").ascending());

        Page<DentistProfile> profiles = dentistProfileRepository
                .findAllByDentistRole(Role.DENTIST, pageable);

        return profiles.map(dentistProfileMapper::toDto);
    }

    @Transactional
    @Override
    public DentistProfileDto updateDentistProfile(Long dentistId, UpdateDentistProfileRequest request) {
        log.debug("Updating dentist profile: dentistId={}", dentistId);

        User dentist = userRepository.findById(dentistId)
                .orElseThrow(() -> new UserNotFoundException("Dentist not found with id: " + dentistId));

        if (!dentist.getRole().equals(Role.DENTIST)) {
            throw new IllegalArgumentException("User is not a dentist");
        }

        DentistProfile profile = dentistProfileRepository.findByDentist(dentist)
                .orElseGet(() -> createDefaultProfile(dentist));

        updateProfileFields(profile, request);

        DentistProfile saved = dentistProfileRepository.save(profile);
        log.info("Dentist profile updated: dentistId={}", dentistId);

        return dentistProfileMapper.toDto(saved);
    }

    @Transactional
    @Override
    public String uploadPhoto(Long dentistId, MultipartFile file) {
        log.debug("Uploading photo for dentist: {}", dentistId);

        validateFile(file);

        User dentist = userRepository.findById(dentistId)
                .orElseThrow(() -> new UserNotFoundException("Dentist not found with id: " + dentistId));

        DentistProfile profile = dentistProfileRepository.findByDentist(dentist)
                .orElseGet(() -> createDefaultProfile(dentist));

        if (profile.getPhotoUrl() != null) {
            deleteOldPhoto(profile.getPhotoUrl());
        }

        String filename = generateUniqueFilename(dentistId, file.getOriginalFilename());
        String photoUrl = saveFile(file, filename);

        profile.setPhotoUrl(photoUrl);
        dentistProfileRepository.save(profile);

        log.info("Photo uploaded for dentist: {}, url: {}", dentistId, photoUrl);
        return photoUrl;
    }

    @Transactional
    @Override
    public void deletePhoto(Long dentistId) {
        log.debug("Deleting photo for dentist: {}", dentistId);

        User dentist = userRepository.findById(dentistId)
                .orElseThrow(() -> new UserNotFoundException("Dentist not found with id: " + dentistId));

        DentistProfile profile = dentistProfileRepository.findByDentist(dentist)
                .orElseThrow(() -> new IllegalStateException("Dentist profile not found"));

        if (profile.getPhotoUrl() != null) {
            deleteOldPhoto(profile.getPhotoUrl());
            profile.setPhotoUrl(null);
            dentistProfileRepository.save(profile);
            log.info("Photo deleted for dentist: {}", dentistId);
        }
    }

    private DentistProfile createDefaultProfile(User dentist) {
        DentistProfile profile = new DentistProfile();
        profile.setDentist(dentist);
        profile.setYearsOfExperience(0);
        profile.setDescription("");
        return dentistProfileRepository.save(profile);
    }

    private void updateProfileFields(DentistProfile profile, UpdateDentistProfileRequest request) {
        if (request.getDescription() != null) {
            profile.setDescription(request.getDescription().trim());
        }
        if (request.getYearsOfExperience() != null) {
            profile.setYearsOfExperience(request.getYearsOfExperience());
        }
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new FileStorageException("File is empty");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new FileStorageException("File size exceeds maximum allowed size");
        }

        String filename = StringUtils.cleanPath(file.getOriginalFilename());
        if (filename.contains("..")) {
            throw new FileStorageException("Invalid file path");
        }

        String extension = getFileExtension(filename).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new FileStorageException("File type not allowed. Allowed types: " +
                    String.join(", ", ALLOWED_EXTENSIONS));
        }
    }

    private String generateUniqueFilename(Long dentistId, String originalFilename) {
        String extension = getFileExtension(originalFilename);
        return "dentist-" + dentistId + "-" + System.currentTimeMillis() + "." + extension;
    }

    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        return lastDotIndex > 0 ? filename.substring(lastDotIndex + 1) : "";
    }

    private String saveFile(MultipartFile file, String filename) {
        try {
            Path targetLocation = uploadDir.resolve(filename);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            return "/uploads/" + filename;
        } catch (IOException ex) {
            throw new FileStorageException("Could not store file " + filename, ex);
        }
    }

    private void deleteOldPhoto(String photoUrl) {
        try {
            String filename = photoUrl.substring(photoUrl.lastIndexOf('/') + 1);
            Path filePath = uploadDir.resolve(filename);
            Files.deleteIfExists(filePath);
        } catch (IOException ex) {
            log.warn("Could not delete old photo file: {}", photoUrl, ex);
        }
    }
}