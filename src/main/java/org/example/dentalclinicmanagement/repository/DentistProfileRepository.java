package org.example.dentalclinicmanagement.repository;

import org.example.dentalclinicmanagement.model.DentistProfile;
import org.example.dentalclinicmanagement.model.Role;
import org.example.dentalclinicmanagement.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface DentistProfileRepository extends JpaRepository<DentistProfile, Long> {

    Optional<DentistProfile> findByDentist(User dentist);

    @Query("SELECT dp FROM DentistProfile dp WHERE dp.dentist.role = :role")
    Page<DentistProfile> findAllByDentistRole(@Param("role") Role role, Pageable pageable);

    @Transactional
    void deleteDentistProfileByDentist(User user);

}
