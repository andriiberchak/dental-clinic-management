package org.example.dentalclinicmanagement.repository;

import org.example.dentalclinicmanagement.model.Role;
import org.example.dentalclinicmanagement.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    Boolean existsByEmail(String email);

    int countByRole(Role role);

    @Query("SELECT u FROM User u WHERE " +
            "(:search IS NULL OR :search = '' OR " +
            " UPPER(u.firstName) LIKE UPPER(CONCAT('%', :search, '%')) OR " +
            " UPPER(u.lastName) LIKE UPPER(CONCAT('%', :search, '%')) OR " +
            " UPPER(CONCAT(u.firstName, ' ', u.lastName)) LIKE UPPER(CONCAT('%', :search, '%')) OR " +
            " UPPER(u.email) LIKE UPPER(CONCAT('%', :search, '%'))) AND " +
            "(:role IS NULL OR u.role = :role)")
    Page<User> findUsersWithFilters(
            @Param("search") String search,
            @Param("role") Role role,
            Pageable pageable);

    Optional<User> findByPhoneNumber(String phoneNumber);

    List<User> findByRole(Role role);
}
 