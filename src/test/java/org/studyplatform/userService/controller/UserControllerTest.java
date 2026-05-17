package org.studyplatform.userService.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.studyplatform.userService.dto.ChangePasswordRequest;
import org.studyplatform.userService.dto.UpdateProfileRequest;
import org.studyplatform.userService.dto.UserSettingsRequest;
import org.studyplatform.userService.entity.Role;
import org.studyplatform.userService.entity.User;
import org.studyplatform.userService.exception.UserNotFoundException;
import org.studyplatform.userService.service.UserService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private UserService userService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        var validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(new UserController(userService))
                .setControllerAdvice(new RestExceptionHandler())
                .setValidator(validator)
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
                .andExpect(jsonPath("$.role").value("STUDENT"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void updateProfile_WhenAuthenticated_ShouldReturnUpdatedCurrentUser() throws Exception {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setFullName("Updated User");
        request.setAvatarUrl("https://example.com/avatar.png");
        request.setBio("Java student");

        User updatedUser = user(1L, "student@example.com", "Updated User", Role.STUDENT);
        updatedUser.setAvatarUrl("https://example.com/avatar.png");
        updatedUser.setBio("Java student");
        when(userService.updateProfile(org.mockito.Mockito.eq("student@example.com"), org.mockito.Mockito.any(UpdateProfileRequest.class)))
                .thenReturn(updatedUser);

        mockMvc.perform(put("/api/v1/users/me/profile")
                        .principal(new TestingAuthenticationToken("student@example.com", null))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.fullName").value("Updated User"))
                .andExpect(jsonPath("$.avatarUrl").value("https://example.com/avatar.png"))
                .andExpect(jsonPath("$.bio").value("Java student"));
    }


    @Test
    void settings_WhenAuthenticated_ShouldReturnCurrentUserSettings() throws Exception {
        User currentUser = user(1L, "student@example.com", "Student User", Role.STUDENT);
        currentUser.setAvatarUrl("https://example.com/avatar.png");
        currentUser.setBio("Java learner");
        currentUser.setPreferredLocale("ru");
        when(userService.findByEmail("student@example.com")).thenReturn(currentUser);

        mockMvc.perform(get("/api/v1/users/me/settings")
                        .principal(new TestingAuthenticationToken("student@example.com", null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("student@example.com"))
                .andExpect(jsonPath("$.fullName").value("Student User"))
                .andExpect(jsonPath("$.role").value("STUDENT"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.avatarUrl").value("https://example.com/avatar.png"))
                .andExpect(jsonPath("$.bio").value("Java learner"))
                .andExpect(jsonPath("$.preferredLocale").value("ru"));
    }

    @Test
    void updateSettings_WhenAuthenticated_ShouldReturnUpdatedSettings() throws Exception {
        UserSettingsRequest request = new UserSettingsRequest();
        request.setFullName("Updated User");
        request.setAvatarUrl("https://example.com/avatar.png");
        request.setBio("Java learner");
        request.setPreferredLocale("en");

        User updatedUser = user(1L, "student@example.com", "Updated User", Role.STUDENT);
        updatedUser.setAvatarUrl("https://example.com/avatar.png");
        updatedUser.setBio("Java learner");
        updatedUser.setPreferredLocale("en");
        when(userService.updateSettings(org.mockito.Mockito.eq("student@example.com"), org.mockito.Mockito.any(UserSettingsRequest.class)))
                .thenReturn(updatedUser);

        mockMvc.perform(put("/api/v1/users/me/settings")
                        .principal(new TestingAuthenticationToken("student@example.com", null))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.fullName").value("Updated User"))
                .andExpect(jsonPath("$.avatarUrl").value("https://example.com/avatar.png"))
                .andExpect(jsonPath("$.bio").value("Java learner"))
                .andExpect(jsonPath("$.preferredLocale").value("en"));
    }

    @Test
    void updateSettings_WithUnsupportedLocale_ShouldReturnBadRequest() throws Exception {
        UserSettingsRequest request = new UserSettingsRequest();
        request.setFullName("Updated User");
        request.setPreferredLocale("de");

        mockMvc.perform(put("/api/v1/users/me/settings")
                        .principal(new TestingAuthenticationToken("student@example.com", null))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("preferredLocale")));
    }

    @Test
    void changePassword_WhenAuthenticated_ShouldReturnNoContent() throws Exception {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("old-password");
        request.setNewPassword("new-password");

        mockMvc.perform(put("/api/v1/users/me/password")
                        .principal(new TestingAuthenticationToken("student@example.com", null))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        org.mockito.Mockito.verify(userService)
                .changePassword(org.mockito.Mockito.eq("student@example.com"), org.mockito.Mockito.any(ChangePasswordRequest.class));
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
                .andExpect(jsonPath("$.role").value("TEACHER"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
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
