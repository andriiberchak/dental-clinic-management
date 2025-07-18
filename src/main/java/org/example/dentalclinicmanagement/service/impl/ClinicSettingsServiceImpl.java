package org.example.dentalclinicmanagement.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.dentalclinicmanagement.dto.ClinicSettingsDto;
import org.example.dentalclinicmanagement.model.ClinicSettings;
import org.example.dentalclinicmanagement.repository.ClinicSettingsRepository;
import org.example.dentalclinicmanagement.service.ClinicSettingsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClinicSettingsServiceImpl implements ClinicSettingsService {

    private final ClinicSettingsRepository repo;

    @Override
    @Transactional(readOnly = true)
    public ClinicSettingsDto getSettings() {
        log.debug("Retrieving clinic settings");

        ClinicSettings settings = repo.findById(1L)
                .orElseGet(() -> {
                    log.info("No clinic settings found, creating default settings");
                    return repo.save(new ClinicSettings());
                });

        return mapToDto(settings);
    }

    @Override
    @Transactional
    public ClinicSettingsDto updateSettings(ClinicSettingsDto newSettings) {
        log.info("Updating clinic settings: {}", newSettings);

        ClinicSettings settings = repo.findById(1L)
                .orElseGet(() -> repo.save(new ClinicSettings()));

        updateSettingsFields(settings, newSettings);

        ClinicSettings savedSettings = repo.save(settings);
        log.info("Clinic settings updated successfully");

        return mapToDto(savedSettings);
    }


    private void updateSettingsFields(ClinicSettings entity, ClinicSettingsDto dto) {
        if (dto.modificationWindowHours() != null) {
            entity.setModificationWindowHours(dto.modificationWindowHours());
        }
        if (dto.dailyChangeLimit() != null) {
            entity.setDailyChangeLimit(dto.dailyChangeLimit());
        }
        if (dto.dailyBookingLimit() != null) {
            entity.setDailyBookingLimit(dto.dailyBookingLimit());
        }
        if (dto.booking24hLimit() != null) {
            entity.setBooking24hLimit(dto.booking24hLimit());
        }
        if (dto.hourlyOverlapLimit() != null) {
            entity.setHourlyOverlapLimit(dto.hourlyOverlapLimit());
        }
    }


    private ClinicSettingsDto mapToDto(ClinicSettings settings) {
        return new ClinicSettingsDto(
                settings.getModificationWindowHours(),
                settings.getDailyChangeLimit(),
                settings.getDailyBookingLimit(),
                settings.getBooking24hLimit(),
                settings.getHourlyOverlapLimit()
        );
    }
}