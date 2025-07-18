package org.example.dentalclinicmanagement.repository;

import org.example.dentalclinicmanagement.model.DentistProfile;
import org.example.dentalclinicmanagement.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface DentistProfileRepository extends JpaRepository<DentistProfile, Long> {
    Optional<DentistProfile> findByDentist(User dentist);

    @Transactional
    void deleteDentistProfileByDentist(User user);

}
