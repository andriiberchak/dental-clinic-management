package org.example.dentalclinicmanagement.service;

import org.example.dentalclinicmanagement.dto.DentistProfileDto;
import org.example.dentalclinicmanagement.dto.request.UpdateDentistProfileRequest;
import org.springframework.data.domain.Page;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

public interface DentistProfileService {

    DentistProfileDto getDentistProfile(Long dentistId);

    @Transactional(readOnly = true)
    Page<DentistProfileDto> getAllDentistProfiles(int page, int size);

    @Transactional
    DentistProfileDto updateDentistProfile(Long dentistId, UpdateDentistProfileRequest request);

    @Transactional
    String uploadPhoto(Long dentistId, MultipartFile file);

    @Transactional
    void deletePhoto(Long dentistId);
}
