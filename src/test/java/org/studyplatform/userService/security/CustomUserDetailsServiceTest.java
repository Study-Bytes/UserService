package org.studyplatform.userService.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.studyplatform.userService.entity.Role;
import org.studyplatform.userService.entity.User;
import org.studyplatform.userService.repository.UserRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {
    @Mock
    private UserRepository userRepository;

    @Test
    void loadUserByUsername_WhenUserExists_ShouldReturnUserDetailsWithRoleAuthority() {
        var user = new User("student@example.com", "encoded-password", Role.STUDENT, "Student User");
        when(userRepository.findByEmail("student@example.com")).thenReturn(Optional.of(user));

        var service = new CustomUserDetailsService(userRepository);
        var userDetails = service.loadUserByUsername("student@example.com");

        assertEquals("student@example.com", userDetails.getUsername());
        assertEquals("encoded-password", userDetails.getPassword());
        assertTrue(userDetails.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_STUDENT")));
    }

    @Test
    void loadUserByUsername_WhenUserMissing_ShouldThrowUsernameNotFoundException() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        var service = new CustomUserDetailsService(userRepository);

        assertThrows(UsernameNotFoundException.class,
                () -> service.loadUserByUsername("missing@example.com"));
    }
}
