package org.example.dentalclinicmanagement.repository;

import org.example.dentalclinicmanagement.model.GoogleCredential;
import org.example.dentalclinicmanagement.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GoogleCredentialRepository extends JpaRepository<GoogleCredential, Long> {
    /* пошук за самим користувачем ─ коли вже є User об’єкт */
    Optional<GoogleCredential> findByUser(User user);

    /* пошук за id користувача (user_id-PK у таблиці users) */
    Optional<GoogleCredential> findByUser_Id(Long userId);

    /* видалення всіх токенів користувача */
    void deleteByUser_Id(Long userId);
}