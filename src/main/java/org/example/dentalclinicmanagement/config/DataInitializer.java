package org.example.dentalclinicmanagement.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.dentalclinicmanagement.model.DentistProfile;
import org.example.dentalclinicmanagement.model.Role;
import org.example.dentalclinicmanagement.model.User;
import org.example.dentalclinicmanagement.repository.DentistProfileRepository;
import org.example.dentalclinicmanagement.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer {

    @Bean
    public CommandLineRunner initData(UserRepository userRepository, PasswordEncoder passwordEncoder,
                                      DentistProfileRepository dentistProfileRepository) {
        return args -> {
            log.info("Starting data initialization...");

            if (!userRepository.existsByEmail("admin@example.com")) {
                User admin = createUser(
                        "admin@example.com",
                        "adminPass",
                        Role.ADMIN,
                        passwordEncoder,
                        "Марія",
                        "Коваленко",
                        "+380632345678"
                );
                userRepository.save(admin);
                log.info("Created admin user: {}", admin.getEmail());
            }

            if (!userRepository.existsByEmail("manager@example.com")) {
                User manager = createUser(
                        "manager@example.com",
                        "managerPass",
                        Role.MANAGER,
                        passwordEncoder,
                        "Андрій",
                        "Сидоренко",
                        "+380501112233"
                );
                userRepository.save(manager);
                log.info("Created manager user: {}", manager.getEmail());
            }

            if (!userRepository.existsByEmail("dentist@example.com")) {
                User dentist = createUser(
                        "dentist@example.com",
                        "dentistPass",
                        Role.DENTIST,
                        passwordEncoder,
                        "Наталія",
                        "Григоренко",
                        "+380931234567"
                );
                User savedDentist = userRepository.save(dentist);
                log.info("Created dentist user: {}", savedDentist.getEmail());

                if (dentistProfileRepository.findByDentist(savedDentist).isEmpty()) {
                    DentistProfile profile = createDentistProfile(savedDentist);
                    dentistProfileRepository.save(profile);
                    log.info("Created dentist profile for: {}", savedDentist.getEmail());
                }
            }

            if (!userRepository.existsByEmail("patient@example.com")) {
                User patient = createUser(
                        "patient@example.com",
                        "patientPass",
                        Role.USER,
                        passwordEncoder,
                        "Олександр",
                        "Іванов",
                        "+380671234567"
                );
                userRepository.save(patient);
                log.info("Created patient user: {}", patient.getEmail());
            }

            log.info("Data initialization completed.");
        };
    }

    private User createUser(String email, String password, Role role, PasswordEncoder passwordEncoder,
                            String firstName, String lastName, String phoneNumber) {
        return User.builder()
                .email(email)
                .password(passwordEncoder.encode(password))
                .firstName(firstName)
                .lastName(lastName)
                .phoneNumber(phoneNumber)
                .role(role)
                .isTwoFactorEnabled(false)
                .twoFactorSecret(null)
                .priorityDentist(null)
                .build();
    }

    private DentistProfile createDentistProfile(User dentist) {
        DentistProfile profile = new DentistProfile();
        profile.setDentist(dentist);
        profile.setDescription("Досвідчений стоматолог з високою кваліфікацією. " +
                "Спеціалізується на терапевтичній стоматології та профілактиці.");
        profile.setYearsOfExperience(5);
        profile.setPhotoUrl(null);
        return profile;
    }
}