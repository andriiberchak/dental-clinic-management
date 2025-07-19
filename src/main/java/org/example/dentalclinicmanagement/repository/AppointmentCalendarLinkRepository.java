package org.example.dentalclinicmanagement.repository;

import org.example.dentalclinicmanagement.model.AppointmentCalendarLink;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AppointmentCalendarLinkRepository extends JpaRepository<AppointmentCalendarLink, Long> {
    Optional<AppointmentCalendarLink> findByAppointmentId(Long id);

    void deleteAllByUserId(Long id);
}