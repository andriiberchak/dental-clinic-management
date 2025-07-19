package org.example.dentalclinicmanagement.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@Table(name = "appointments")
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dentist_id", nullable = false)
    private User dentist;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private User client;

    @Column(name = "appointment_time", nullable = false)
    private LocalDateTime appointmentTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private AppointmentStatus status;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Column(length = 1000)
    private String comment;
    @Column(name = "last_change_date")
    private LocalDate lastChangeDate;

    @Column(name = "daily_change_count", nullable = false)
    private int dailyChangeCount = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
    @Column(name = "reminder_sent", nullable = false, columnDefinition = "boolean default false")
    private boolean reminderSent = false;
    public Appointment(User dentist, LocalDateTime appointmentTime, AppointmentStatus status) {
        this.dentist = dentist;
        this.appointmentTime = appointmentTime;
        this.status = status;
    }
}
