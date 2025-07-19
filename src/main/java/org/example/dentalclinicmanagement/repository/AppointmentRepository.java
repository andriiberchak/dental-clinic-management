package org.example.dentalclinicmanagement.repository;

import org.example.dentalclinicmanagement.model.Appointment;
import org.example.dentalclinicmanagement.model.AppointmentStatus;
import org.example.dentalclinicmanagement.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    List<Appointment> findByDentistAndAppointmentTimeBetween(User user, LocalDateTime start, LocalDateTime end);

    Optional<Appointment> findByDentistAndAppointmentTime(User dentist, LocalDateTime appointmentTime);

    List<Appointment> findByClient(User user);

    long countByClientAndStatusAndAppointmentTimeBetween(
            User client,
            AppointmentStatus status,
            LocalDateTime start,
            LocalDateTime end
    );

    long countByClientAndCreatedAtBetween(User client,
                                          LocalDateTime start,
                                          LocalDateTime end);
}
