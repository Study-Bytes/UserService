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
import org.studyplatform.userService.entity.Role;
import org.studyplatform.userService.entity.User;
import org.studyplatform.userService.exception.EmailAlreadyTakenException;
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
        assertEquals("Student Test", result.getFullName());
        verify(passwordEncoder).encode("password123");
        verify(userRepository).save(any(User.class));
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
}
