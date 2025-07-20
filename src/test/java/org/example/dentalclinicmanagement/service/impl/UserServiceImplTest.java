package org.example.dentalclinicmanagement.service.impl;

import org.example.dentalclinicmanagement.dto.DentistProfileDto;
import org.example.dentalclinicmanagement.dto.MinimalUserRegistrationDTO;
import org.example.dentalclinicmanagement.dto.UserDto;
import org.example.dentalclinicmanagement.dto.request.ChangePasswordRequest;
import org.example.dentalclinicmanagement.dto.request.ForgotPasswordRequest;
import org.example.dentalclinicmanagement.dto.request.ResetPasswordRequest;
import org.example.dentalclinicmanagement.dto.request.UpdateUserProfileRequest;
import org.example.dentalclinicmanagement.dto.response.MessageResponse;
import org.example.dentalclinicmanagement.exception.PasswordUpdateException;
import org.example.dentalclinicmanagement.exception.ResetPasswordException;
import org.example.dentalclinicmanagement.exception.UserNotFoundException;
import org.example.dentalclinicmanagement.mapper.UserMapper;
import org.example.dentalclinicmanagement.model.*;
import org.example.dentalclinicmanagement.repository.DentistProfileRepository;
import org.example.dentalclinicmanagement.repository.PasswordResetTokenRepository;
import org.example.dentalclinicmanagement.repository.UserRepository;
import org.example.dentalclinicmanagement.util.EmailUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private DentistProfileRepository dentistProfileRepository;

    @Mock
    private EmailUtil emailUtil;

    @InjectMocks
    private UserServiceImpl userService;

    private User user;
    private User dentist;
    private UserDto userDto;
    private DentistProfile dentistProfile;
    private UpdateUserProfileRequest updateRequest;
    private ChangePasswordRequest changePasswordRequest;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setEmail("user@example.com");
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setPhoneNumber("+1234567890");
        user.setPassword("encoded-password");
        user.setRole(Role.USER);

        dentist = new User();
        dentist.setId(2L);
        dentist.setEmail("dentist@example.com");
        dentist.setFirstName("Dr. Jane");
        dentist.setLastName("Smith");
        dentist.setRole(Role.DENTIST);

        userDto = new UserDto();
        userDto.setId(1L);
        userDto.setEmail("user@example.com");

        dentistProfile = new DentistProfile();
        dentistProfile.setId(1L);
        dentistProfile.setDentist(dentist);
        dentistProfile.setDescription("Experienced dentist");
        dentistProfile.setYearsOfExperience(10);

        updateRequest = UpdateUserProfileRequest.builder()
                .firstName("Updated")
                .lastName("Name")
                .phoneNumber("+0987654321")
                .email("updated@example.com")
                .build();

        changePasswordRequest = new ChangePasswordRequest();
        changePasswordRequest.setCurrentPassword("oldPassword");
        changePasswordRequest.setNewPassword("newPassword");
        changePasswordRequest.setConfirmPassword("newPassword");

        ReflectionTestUtils.setField(userService, "frontendUrl", "http://localhost:3000");
    }

    @Test
    void getUserByEmail_Success() {
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(userMapper.toUserDTO(user)).thenReturn(userDto);

        UserDto result = userService.getUserByEmail("user@example.com");

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("user@example.com", result.getEmail());
        verify(userRepository).findByEmail("user@example.com");
        verify(userMapper).toUserDTO(user);
    }

    @Test
    void getUserByEmail_NotFound_ThrowsException() {
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        UserNotFoundException exception = assertThrows(
                UserNotFoundException.class,
                () -> userService.getUserByEmail("nonexistent@example.com")
        );

        assertEquals("User not found with email: nonexistent@example.com", exception.getMessage());
    }

    @Test
    void getUserById_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userMapper.toUserDTO(user)).thenReturn(userDto);

        UserDto result = userService.getUserById(1L);

        assertNotNull(result);
        verify(userRepository).findById(1L);
        verify(userMapper).toUserDTO(user);
    }

    @Test
    void updateUserRole_UserToDentist_CreatesProfile() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.countByRole(Role.ADMIN)).thenReturn(2);
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(dentistProfileRepository.findByDentist(user)).thenReturn(Optional.empty());
        when(dentistProfileRepository.save(any(DentistProfile.class))).thenReturn(dentistProfile);
        when(userMapper.toUserDTO(user)).thenReturn(userDto);

        UserDto result = userService.updateUserRole(1L, Role.DENTIST);

        assertNotNull(result);
        verify(userRepository).save(argThat(savedUser -> savedUser.getRole().equals(Role.DENTIST)));
        verify(dentistProfileRepository).save(argThat(profile ->
                profile.getDentist().equals(user) &&
                        profile.getDescription().equals("Default description") &&
                        profile.getYearsOfExperience().equals(0)
        ));
    }

    @Test
    void updateUserRole_DentistToUser_RemovesProfile() {
        dentist.setRole(Role.DENTIST);
        when(userRepository.findById(2L)).thenReturn(Optional.of(dentist));
        when(userRepository.countByRole(Role.ADMIN)).thenReturn(2);
        when(userRepository.save(any(User.class))).thenReturn(dentist);
        when(userMapper.toUserDTO(dentist)).thenReturn(userDto);

        UserDto result = userService.updateUserRole(2L, Role.USER);

        assertNotNull(result);
        verify(dentistProfileRepository).deleteDentistProfileByDentist(dentist);
        verify(userRepository).save(argThat(savedUser -> savedUser.getRole().equals(Role.USER)));
    }

    @Test
    void updateUserRole_LastAdmin_ThrowsException() {
        User admin = new User();
        admin.setId(1L);
        admin.setRole(Role.ADMIN);

        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(userRepository.countByRole(Role.ADMIN)).thenReturn(1);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userService.updateUserRole(1L, Role.USER)
        );

        assertEquals("Cannot change role of the last admin user", exception.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateUserProfile_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(userMapper.toUserDTO(user)).thenReturn(userDto);

        UserDto result = userService.updateUserProfile(1L, updateRequest);

        assertNotNull(result);
        verify(userRepository).save(argThat(savedUser ->
                savedUser.getFirstName().equals("Updated") &&
                        savedUser.getLastName().equals("Name") &&
                        savedUser.getPhoneNumber().equals("+0987654321")
        ));
    }

    @Test
    void changePassword_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("oldPassword", "encoded-password")).thenReturn(true);
        when(passwordEncoder.matches("newPassword", "encoded-password")).thenReturn(false);
        when(passwordEncoder.encode("newPassword")).thenReturn("new-encoded-password");
        when(userRepository.save(any(User.class))).thenReturn(user);

        MessageResponse result = userService.changePassword(1L, changePasswordRequest);

        assertNotNull(result);
        assertEquals("Password changed successfully", result.getMessage());
        verify(userRepository).save(argThat(savedUser ->
                savedUser.getPassword().equals("new-encoded-password")
        ));
    }

    @Test
    void changePassword_PasswordsDoNotMatch_ThrowsException() {
        changePasswordRequest.setConfirmPassword("differentPassword");

        PasswordUpdateException exception = assertThrows(
                PasswordUpdateException.class,
                () -> userService.changePassword(1L, changePasswordRequest)
        );

        assertEquals("update.passwords.do.not.match", exception.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void changePassword_IncorrectCurrentPassword_ThrowsException() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("oldPassword", "encoded-password")).thenReturn(false);

        PasswordUpdateException exception = assertThrows(
                PasswordUpdateException.class,
                () -> userService.changePassword(1L, changePasswordRequest)
        );

        assertEquals("update.current.password.incorrect", exception.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void changePassword_SamePassword_ThrowsException() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("oldPassword", "encoded-password")).thenReturn(true);
        when(passwordEncoder.matches("newPassword", "encoded-password")).thenReturn(true);

        PasswordUpdateException exception = assertThrows(
                PasswordUpdateException.class,
                () -> userService.changePassword(1L, changePasswordRequest)
        );

        assertEquals("update.new.password.must.be.different", exception.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void generatePasswordResetToken_Success() {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("user@example.com");

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(passwordResetTokenRepository.save(any(PasswordResetToken.class)))
                .thenReturn(new PasswordResetToken());

        userService.generatePasswordResetToken(request);

        verify(passwordResetTokenRepository).save(argThat(token ->
                token.getUser().equals(user) &&
                        token.getToken() != null &&
                        token.getExpiryDate().isAfter(Instant.now())
        ));
        verify(emailUtil).sendPasswordResetEmail(eq("user@example.com"), anyString());
    }

    @Test
    void resetPassword_Success() {
        String tokenValue = "reset-token";
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken(tokenValue);
        request.setNewPassword("newPassword123");

        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setToken(tokenValue);
        resetToken.setUser(user);
        resetToken.setUsed(false);
        resetToken.setExpiryDate(Instant.now().plus(1, ChronoUnit.HOURS));

        when(passwordResetTokenRepository.findByToken(tokenValue)).thenReturn(Optional.of(resetToken));
        when(passwordEncoder.encode("newPassword123")).thenReturn("new-encoded-password");
        when(passwordResetTokenRepository.save(any(PasswordResetToken.class))).thenReturn(resetToken);
        when(userRepository.save(any(User.class))).thenReturn(user);

        userService.resetPassword(request);

        verify(passwordEncoder).encode("newPassword123");
        verify(userRepository).save(argThat(savedUser ->
                savedUser.getPassword().equals("new-encoded-password")
        ));
        verify(passwordResetTokenRepository).save(argThat(PasswordResetToken::isUsed));
    }

    @Test
    void resetPassword_InvalidToken_ThrowsException() {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("invalid-token");
        request.setNewPassword("newPassword123");

        when(passwordResetTokenRepository.findByToken("invalid-token")).thenReturn(Optional.empty());

        ResetPasswordException exception = assertThrows(
                ResetPasswordException.class,
                () -> userService.resetPassword(request)
        );

        assertEquals("reset.password.invalid", exception.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void resetPassword_ExpiredToken_ThrowsException() {
        String tokenValue = "expired-token";
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken(tokenValue);
        request.setNewPassword("newPassword123");

        PasswordResetToken expiredToken = new PasswordResetToken();
        expiredToken.setToken(tokenValue);
        expiredToken.setUser(user);
        expiredToken.setUsed(false);
        expiredToken.setExpiryDate(Instant.now().minus(1, ChronoUnit.HOURS));
        when(passwordResetTokenRepository.findByToken(tokenValue)).thenReturn(Optional.of(expiredToken));

        ResetPasswordException exception = assertThrows(
                ResetPasswordException.class,
                () -> userService.resetPassword(request)
        );

        assertEquals("reset.password.token.expired", exception.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void resetPassword_UsedToken_ThrowsException() {
        String tokenValue = "used-token";
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken(tokenValue);
        request.setNewPassword("newPassword123");

        PasswordResetToken usedToken = new PasswordResetToken();
        usedToken.setToken(tokenValue);
        usedToken.setUser(user);
        usedToken.setUsed(true);
        usedToken.setExpiryDate(Instant.now().plus(1, ChronoUnit.HOURS));

        when(passwordResetTokenRepository.findByToken(tokenValue)).thenReturn(Optional.of(usedToken));

        ResetPasswordException exception = assertThrows(
                ResetPasswordException.class,
                () -> userService.resetPassword(request)
        );

        assertEquals("reset.password.token.already.used", exception.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void updatePriorityDentist_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(dentistProfileRepository.findById(1L)).thenReturn(Optional.of(dentistProfile));
        when(userRepository.findById(2L)).thenReturn(Optional.of(dentist));
        when(userRepository.save(any(User.class))).thenReturn(user);

        MessageResponse result = userService.updatePriorityDentist(1L, 1L);

        assertNotNull(result);
        assertEquals("Priority dentist updated successfully!", result.getMessage());
        verify(userRepository).save(argThat(savedUser ->
                savedUser.getPriorityDentist().equals(dentist)
        ));
    }

    @Test
    void updatePriorityDentist_NotDentist_ThrowsException() {
        User nonDentist = new User();
        nonDentist.setId(3L);
        nonDentist.setRole(Role.USER);

        DentistProfile profile = new DentistProfile();
        profile.setDentist(nonDentist);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(dentistProfileRepository.findById(1L)).thenReturn(Optional.of(profile));
        when(userRepository.findById(3L)).thenReturn(Optional.of(nonDentist));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userService.updatePriorityDentist(1L, 1L)
        );

        assertEquals("Selected user is not a dentist", exception.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void getPriorityDentistProfile_Success() {
        user.setPriorityDentist(dentist);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(dentistProfileRepository.findByDentist(dentist)).thenReturn(Optional.of(dentistProfile));

        DentistProfileDto result = userService.getPriorityDentistProfile(1L);

        assertNotNull(result);
        assertEquals(dentist.getId(), result.getDentistId());
        assertEquals(dentist.getFirstName(), result.getFirstName());
        assertEquals(dentist.getLastName(), result.getLastName());
        assertEquals(dentistProfile.getDescription(), result.getDescription());
        assertEquals(dentistProfile.getYearsOfExperience(), result.getExperience());
    }

    @Test
    void getPriorityDentistProfile_NoPriorityDentist_ReturnsNull() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        DentistProfileDto result = userService.getPriorityDentistProfile(1L);

        assertNull(result);
        verify(dentistProfileRepository, never()).findByDentist(any());
    }

    @Test
    void registerMinimalUser_Success() {
        MinimalUserRegistrationDTO dto = new MinimalUserRegistrationDTO();
        dto.setPhone("+1234567890");
        dto.setFirstName("John");
        dto.setLastName("Doe");

        when(userRepository.existsByPhoneNumber("+1234567890")).thenReturn(false);
        when(passwordEncoder.encode("defaultPassword")).thenReturn("encoded-default-password");
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(userMapper.toUserDTO(user)).thenReturn(userDto);

        UserDto result = userService.registerMinimalUser(dto);

        assertNotNull(result);
        verify(userRepository).save(argThat(savedUser ->
                savedUser.getPhoneNumber().equals("+1234567890") &&
                        savedUser.getFirstName().equals("John") &&
                        savedUser.getLastName().equals("Doe") &&
                        savedUser.getEmail().equals("+1234567890@phone.com") &&
                        savedUser.getPassword().equals("encoded-default-password") &&
                        savedUser.getRole().equals(Role.USER)
        ));
    }

    @Test
    void registerMinimalUser_PhoneAlreadyExists_ThrowsException() {
        MinimalUserRegistrationDTO dto = new MinimalUserRegistrationDTO();
        dto.setPhone("+1234567890");
        dto.setFirstName("John");
        dto.setLastName("Doe");

        when(userRepository.existsByPhoneNumber("+1234567890")).thenReturn(true);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> userService.registerMinimalUser(dto)
        );

        assertEquals("User already exist!", exception.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateUserProfile_PartialUpdate_OnlyUpdatesNonNullFields() {
        UpdateUserProfileRequest partialRequest = UpdateUserProfileRequest.builder()
                .firstName("UpdatedName")
                .lastName(null).phoneNumber("+9999999999")
                .email(null).build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(userMapper.toUserDTO(user)).thenReturn(userDto);

        UserDto result = userService.updateUserProfile(1L, partialRequest);

        assertNotNull(result);
        verify(userRepository).save(argThat(savedUser ->
                savedUser.getFirstName().equals("UpdatedName") &&
                        savedUser.getLastName().equals("Doe") && savedUser.getPhoneNumber().equals("+9999999999")
        ));
    }

    @Test
    void updateUserProfile_EmptyStrings_IgnoresEmptyValues() {
        UpdateUserProfileRequest emptyRequest = UpdateUserProfileRequest.builder()
                .firstName("").lastName("   ")
                .phoneNumber("ValidPhone")
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(userMapper.toUserDTO(user)).thenReturn(userDto);

        UserDto result = userService.updateUserProfile(1L, emptyRequest);

        assertNotNull(result);
        verify(userRepository).save(argThat(savedUser ->
                savedUser.getFirstName().equals("John") && savedUser.getLastName().equals("Doe") &&
                        savedUser.getPhoneNumber().equals("ValidPhone")
        ));
    }
}