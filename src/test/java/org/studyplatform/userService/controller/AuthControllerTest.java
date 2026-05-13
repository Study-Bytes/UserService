package org.studyplatform.userService.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.studyplatform.userService.dto.AuthResponse;
import org.studyplatform.userService.dto.LoginRequest;
import org.studyplatform.userService.dto.RefreshRequest;
import org.studyplatform.userService.dto.RegisterRequest;
import org.studyplatform.userService.entity.Role;
import org.studyplatform.userService.entity.User;
import org.studyplatform.userService.exception.InvalidTokenException;
import org.studyplatform.userService.service.AuthService;
import org.studyplatform.userService.service.UserService;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private AuthService authService;

    @Mock
    private UserService userService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        var validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(authService, userService))
                .setControllerAdvice(new RestExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void register_WithValidRequest_ShouldReturnCreatedMessage() throws Exception {
        var savedUser = new User("user@example.com", "encoded-password", Role.STUDENT, "User Name");
        savedUser.setId(10L);
        when(userService.register(any(RegisterRequest.class))).thenReturn(savedUser);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(registerRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("User registered successfully"))
                .andExpect(jsonPath("$.userId").value(10));
    }

    @Test
    void register_WithInvalidRequest_ShouldReturnBadRequest() throws Exception {
        var request = registerRequest();
        request.setEmail("bad-email");
        request.setPassword("short");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("email")));
    }

    @Test
    void login_WithValidCredentials_ShouldReturnAuthResponse() throws Exception {
        var response = new AuthResponse("access-token", "refresh-token", "Bearer", 900L);
        when(authService.login(any(LoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(loginRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900));
    }

    @Test
    void login_WithBadCredentials_ShouldReturnUnauthorized() throws Exception {
        when(authService.login(any(LoginRequest.class))).thenThrow(new BadCredentialsException("Bad credentials"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(loginRequest())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void refresh_WithValidToken_ShouldReturnAccessToken() throws Exception {
        when(authService.refreshAccessToken(any(RefreshRequest.class))).thenReturn("new-access-token");

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(refreshRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Access token refreshed successfully"))
                .andExpect(jsonPath("$.accessToken").value("new-access-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    void refresh_WithInvalidToken_ShouldReturnUnauthorized() throws Exception {
        when(authService.refreshAccessToken(any(RefreshRequest.class)))
                .thenThrow(new InvalidTokenException("Invalid or expired refresh token"));

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(refreshRequest())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid or expired refresh token"));
    }

    @Test
    void logout_WithValidToken_ShouldReturnSuccessMessage() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(refreshRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logged out successfully"));
    }

    @Test
    void logout_WithInvalidToken_ShouldReturnUnauthorized() throws Exception {
        doThrow(new InvalidTokenException("Refresh token not found"))
                .when(authService).logout("refresh-token");

        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(refreshRequest())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Refresh token not found"));
    }

    private RegisterRequest registerRequest() {
        var request = new RegisterRequest();
        request.setEmail("user@example.com");
        request.setPassword("strong-password");
        request.setFullName("User Name");
        return request;
    }

    private LoginRequest loginRequest() {
        var request = new LoginRequest();
        request.setEmail("user@example.com");
        request.setPassword("strong-password");
        return request;
    }

    private RefreshRequest refreshRequest() {
        var request = new RefreshRequest();
        request.setRefreshToken("refresh-token");
        return request;
    }
}
