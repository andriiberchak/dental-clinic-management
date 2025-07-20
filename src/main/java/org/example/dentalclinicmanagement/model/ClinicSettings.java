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

    @Column(name = "booking_24h_limit", nullable = false)
    private Integer booking24hLimit = 3;

    @Column(name = "daily_booking_limit", nullable = false)
    private Integer dailyBookingLimit = 1;

    @Column(name = "daily_change_limit", nullable = false)
    private Integer dailyChangeLimit = 3;

    @Column(name = "hourly_overlap_limit", nullable = false)
    private Integer hourlyOverlapLimit = 1;

    @Column(name = "modification_window_hours", nullable = false)
    private Integer modificationWindowHours = 2;
}
