package org.example.dentalclinicmanagement.repository;

import org.example.dentalclinicmanagement.model.GoogleCredential;
import org.example.dentalclinicmanagement.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GoogleCredentialRepository extends JpaRepository<GoogleCredential, Long> {
    Optional<GoogleCredential> findByUser(User user);

    Optional<GoogleCredential> findByUser_Id(Long userId);

}