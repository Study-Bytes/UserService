package org.studyplatform.userService.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.studyplatform.userService.dto.RegisterRequest;
import org.studyplatform.userService.dto.ChangePasswordRequest;
import org.studyplatform.userService.dto.UpdateProfileRequest;
import org.studyplatform.userService.entity.Role;
import org.studyplatform.userService.entity.User;
import org.studyplatform.userService.entity.UserStatus;
import org.studyplatform.userService.exception.EmailAlreadyTakenException;
import org.studyplatform.userService.exception.InvalidUserRoleException;
import org.studyplatform.userService.exception.UserNotFoundException;
import org.studyplatform.userService.repository.UserRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private RegisterRequest request;

    @BeforeEach
    void setUp() {
        request = new RegisterRequest();
        request.setEmail("student@test.com");
        request.setPassword("password123");
        request.setFullName("Student Test");
    }

    @Test
    void register_ShouldCreateUserWithEncodedPasswordAndStudentRole() {
        request.setRole(null);
        when(userRepository.findByEmail("student@test.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });

        User result = userService.register(request);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("student@test.com", result.getEmail());
        assertEquals("encoded-password", result.getPassword());
        assertEquals(Role.STUDENT, result.getRole());
        assertEquals(UserStatus.ACTIVE, result.getStatus());
        assertEquals("Student Test", result.getFullName());
        verify(passwordEncoder).encode("password123");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_WithTeacherRole_ShouldCreateTeacherUser() {
        request.setRole(Role.TEACHER);
        when(userRepository.findByEmail("student@test.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.register(request);

        assertEquals(Role.TEACHER, result.getRole());
    }

    @Test
    void register_WithAdminRole_ShouldRejectSelfServiceRegistration() {
        request.setRole(Role.ADMIN);
        when(userRepository.findByEmail("student@test.com")).thenReturn(Optional.empty());

        assertThrows(InvalidUserRoleException.class, () -> userService.register(request));

        verify(userRepository, never()).save(any(User.class));
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void register_ShouldRejectExistingEmailBeforeSave() {
        when(userRepository.findByEmail("student@test.com")).thenReturn(Optional.of(new User()));

        assertThrows(EmailAlreadyTakenException.class, () -> userService.register(request));

        verify(userRepository, never()).save(any(User.class));
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void register_ShouldConvertDatabaseDuplicateErrorToDomainException() {
        when(userRepository.findByEmail("student@test.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenThrow(new DataIntegrityViolationException("duplicate email"));

        assertThrows(EmailAlreadyTakenException.class, () -> userService.register(request));
    }

    @Test
    void findByEmail_ShouldReturnUser() {
        User user = new User();
        user.setEmail("student@test.com");
        when(userRepository.findByEmail("student@test.com")).thenReturn(Optional.of(user));

        User result = userService.findByEmail("student@test.com");

        assertEquals("student@test.com", result.getEmail());
    }

    @Test
    void findByEmail_ShouldThrowWhenUserMissing() {
        when(userRepository.findByEmail("missing@test.com")).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.findByEmail("missing@test.com"));
    }

    @Test
    void findById_ShouldReturnUser() {
        User user = new User();
        user.setId(5L);
        when(userRepository.findById(5L)).thenReturn(Optional.of(user));

        User result = userService.findById(5L);

        assertEquals(5L, result.getId());
    }

    @Test
    void findById_ShouldThrowWhenUserMissing() {
        when(userRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.findById(404L));
    }

    @Test
    void updateProfile_ShouldSaveProfileFields() {
        User user = new User();
        user.setId(7L);
        user.setEmail("student@test.com");
        when(userRepository.findByEmail("student@test.com")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateProfileRequest profileRequest = new UpdateProfileRequest();
        profileRequest.setFullName("Updated Student");
        profileRequest.setAvatarUrl("https://example.com/avatar.png");
        profileRequest.setBio("Java student");

        User result = userService.updateProfile("student@test.com", profileRequest);

        assertEquals("Updated Student", result.getFullName());
        assertEquals("https://example.com/avatar.png", result.getAvatarUrl());
        assertEquals("Java student", result.getBio());
        verify(userRepository).save(user);
    }

    @Test
    void changePassword_ShouldVerifyCurrentPasswordAndSaveNewHash() {
        User user = new User();
        user.setId(8L);
        user.setEmail("student@test.com");
        user.setPassword("old-hash");
        when(userRepository.findByEmail("student@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("old-password", "old-hash")).thenReturn(true);
        when(passwordEncoder.encode("new-password")).thenReturn("new-hash");

        ChangePasswordRequest passwordRequest = new ChangePasswordRequest();
        passwordRequest.setCurrentPassword("old-password");
        passwordRequest.setNewPassword("new-password");

        userService.changePassword("student@test.com", passwordRequest);

        assertEquals("new-hash", user.getPassword());
        verify(userRepository).save(user);
    }
}
