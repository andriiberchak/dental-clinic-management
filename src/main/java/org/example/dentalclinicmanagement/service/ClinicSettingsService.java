package org.example.dentalclinicmanagement.service;

import org.example.dentalclinicmanagement.dto.ClinicSettingsDto;

public interface ClinicSettingsService {
    ClinicSettingsDto getSettings();

    ClinicSettingsDto updateSettings(ClinicSettingsDto newSettings);
}
