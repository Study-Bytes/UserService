package org.studyplatform.userService.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.studyplatform.userService.entity.Role;
import org.studyplatform.userService.entity.User;
import org.studyplatform.userService.exception.UserNotFoundException;
import org.studyplatform.userService.service.UserService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {
    @Mock
    private UserService userService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new UserController(userService))
                .setControllerAdvice(new RestExceptionHandler())
                .build();
    }

    @Test
    void me_WhenAuthenticated_ShouldReturnCurrentUserProfile() throws Exception {
        when(userService.findByEmail("student@example.com")).thenReturn(user(
                1L,
                "student@example.com",
                "Student User",
                Role.STUDENT
        ));

        mockMvc.perform(get("/api/v1/users/me")
                        .principal(new TestingAuthenticationToken("student@example.com", null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("student@example.com"))
                .andExpect(jsonPath("$.fullName").value("Student User"))
                .andExpect(jsonPath("$.role").value("STUDENT"));
    }

    @Test
    void getById_WhenUserExists_ShouldReturnUserDto() throws Exception {
        when(userService.findById(2L)).thenReturn(user(
                2L,
                "teacher@example.com",
                "Teacher User",
                Role.TEACHER
        ));

        mockMvc.perform(get("/api/v1/users/{id}", 2L)
                        .principal(new TestingAuthenticationToken("admin@example.com", null, "ROLE_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.email").value("teacher@example.com"))
                .andExpect(jsonPath("$.fullName").value("Teacher User"))
                .andExpect(jsonPath("$.role").value("TEACHER"));
    }

    @Test
    void getById_WhenUserMissing_ShouldReturnNotFound() throws Exception {
        when(userService.findById(99L)).thenThrow(new UserNotFoundException("User not found"));

        mockMvc.perform(get("/api/v1/users/{id}", 99L)
                        .principal(new TestingAuthenticationToken("admin@example.com", null, "ROLE_ADMIN")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User not found"));
    }

    @Test
    void getById_ShouldBeRestrictedToAdminUsers() throws Exception {
        var method = UserController.class.getMethod("getById", Long.class);
        var preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertNotNull(preAuthorize);
        assertEquals("hasRole('ADMIN')", preAuthorize.value());
    }

    private User user(Long id, String email, String fullName, Role role) {
        var user = new User(email, "encoded-password", role, fullName);
        user.setId(id);
        return user;
    }
}
