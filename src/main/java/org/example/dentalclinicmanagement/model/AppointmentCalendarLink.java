// NEW  src/main/java/com/secure/notes/models/AppointmentCalendarLink.java
package org.example.dentalclinicmanagement.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity @Data
@Table(name = "appointment_calendar_link")
public class AppointmentCalendarLink {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)   // ID запису у вашій БД
    private Long appointmentId;

    @Column(nullable = false)   // eventId у Google Calendar
    private String googleEventId;

    private Long userId;                     // ← NEW
}
