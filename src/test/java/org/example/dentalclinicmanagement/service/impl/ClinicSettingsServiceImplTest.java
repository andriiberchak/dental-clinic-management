package org.example.dentalclinicmanagement.service.impl;

import org.example.dentalclinicmanagement.dto.ClinicSettingsDto;
import org.example.dentalclinicmanagement.model.ClinicSettings;
import org.example.dentalclinicmanagement.repository.ClinicSettingsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClinicSettingsServiceImplTest {

    @Mock
    private ClinicSettingsRepository repository;

    @InjectMocks
    private ClinicSettingsServiceImpl clinicSettingsService;

    private ClinicSettings existingSettings;
    private ClinicSettingsDto settingsDto;

    @BeforeEach
    void setUp() {
        existingSettings = new ClinicSettings();
        existingSettings.setId(1L);
        existingSettings.setModificationWindowHours(24);
        existingSettings.setDailyChangeLimit(3);
        existingSettings.setDailyBookingLimit(5);
        existingSettings.setBooking24hLimit(10);
        existingSettings.setHourlyOverlapLimit(1);

        settingsDto = new ClinicSettingsDto(
                15,
                8,
                5,
                2,
                48
        );
    }

    @Test
    void getSettings_ExistingSettings_ReturnsDto() {
        when(repository.findById(1L)).thenReturn(Optional.of(existingSettings));

        ClinicSettingsDto result = clinicSettingsService.getSettings();

        assertNotNull(result);
        assertEquals(1, result.modificationWindowHours());
        assertEquals(5, result.dailyChangeLimit());
        assertEquals(3, result.dailyBookingLimit());
        assertEquals(24, result.booking24hLimit());
        assertEquals(10, result.hourlyOverlapLimit());

        verify(repository).findById(1L);
        verify(repository, never()).save(any());
    }

    @Test
    void getSettings_NoExistingSettings_CreatesDefaultAndReturns() {
        ClinicSettings defaultSettings = new ClinicSettings();
        defaultSettings.setId(1L);

        when(repository.findById(1L)).thenReturn(Optional.empty());
        when(repository.save(any(ClinicSettings.class))).thenReturn(defaultSettings);

        ClinicSettingsDto result = clinicSettingsService.getSettings();

        assertNotNull(result);
        verify(repository).findById(1L);
        verify(repository).save(any(ClinicSettings.class));
    }

    @Test
    void updateSettings_ExistingSettings_UpdatesAllFields() {
        when(repository.findById(1L)).thenReturn(Optional.of(existingSettings));
        when(repository.save(any(ClinicSettings.class))).thenReturn(existingSettings);

        ClinicSettingsDto result = clinicSettingsService.updateSettings(settingsDto);

        assertNotNull(result);
        assertEquals(48, result.booking24hLimit());
        assertEquals(5, result.dailyBookingLimit());
        assertEquals(8, result.dailyChangeLimit());
        assertEquals(15, result.hourlyOverlapLimit());
        assertEquals(2, result.modificationWindowHours());

        verify(repository).findById(1L);
        verify(repository).save(argThat(settings ->
                settings.getModificationWindowHours().equals(48) &&
                        settings.getDailyChangeLimit().equals(5) &&
                        settings.getDailyBookingLimit().equals(8) &&
                        settings.getBooking24hLimit().equals(15) &&
                        settings.getHourlyOverlapLimit().equals(2)
        ));
    }

    @Test
    void updateSettings_NoExistingSettings_CreatesNewAndUpdates() {
        ClinicSettings newSettings = new ClinicSettings();
        when(repository.findById(1L)).thenReturn(Optional.empty());
        when(repository.save(any(ClinicSettings.class)))
                .thenReturn(newSettings).thenReturn(newSettings);
        ClinicSettingsDto result = clinicSettingsService.updateSettings(settingsDto);

        assertNotNull(result);
        verify(repository).findById(1L);
        verify(repository, times(2)).save(any(ClinicSettings.class));
    }

    @Test
    void updateSettings_ValidatesBusinessLogic() {
        when(repository.findById(1L)).thenReturn(Optional.of(existingSettings));
        when(repository.save(any(ClinicSettings.class))).thenReturn(existingSettings);

        clinicSettingsService.updateSettings(settingsDto);

        verify(repository).save(any(ClinicSettings.class));
    }
}