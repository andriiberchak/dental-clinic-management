package org.example.dentalclinicmanagement.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "clinic_settings")
@Data
public class ClinicSettings {

    @Id
    @Column(name = "id")
    private Long id = 1L;

    @Column(name = "modification_window_hours", nullable = false)
    private int modificationWindowHours = 2;

    @Column(name = "daily_change_limit", nullable = false)
    private int dailyChangeLimit = 3;

    @Column(name = "daily_booking_limit", nullable = false)
    private int dailyBookingLimit = 1;

    @Column(name = "booking_24h_limit", nullable = false)
    private int booking24hLimit = 3;

    @Column(name = "hourly_overlap_limit", nullable = false)
    private int hourlyOverlapLimit = 1;
}
