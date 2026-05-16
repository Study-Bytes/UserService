package org.studyplatform.userService.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.studyplatform.userService.dto.AuthResponse;
import org.studyplatform.userService.dto.CurrentUser;
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
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
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
    void register_WithValidRequest_ShouldReturnAuthResponse() throws Exception {
        var savedUser = new User("user@example.com", "encoded-password", Role.STUDENT, "User Name");
        savedUser.setId(10L);
        when(userService.register(any(RegisterRequest.class))).thenReturn(savedUser);
        when(authService.issueTokenPair(savedUser)).thenReturn(authResponse(savedUser));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(registerRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.id").value(10))
                .andExpect(jsonPath("$.user.email").value("user@example.com"))
                .andExpect(jsonPath("$.user.status").value("ACTIVE"))
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900));
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
        var user = new User("user@example.com", "encoded-password", Role.STUDENT, "User Name");
        user.setId(1L);
        var response = authResponse(user);
        when(authService.login(any(LoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(loginRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.email").value("user@example.com"))
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
    void refresh_WithValidToken_ShouldReturnAuthResponse() throws Exception {
        var user = new User("user@example.com", "encoded-password", Role.STUDENT, "User Name");
        user.setId(1L);
        var response = new AuthResponse(CurrentUser.from(user), "new-access-token", "refresh-token", "Bearer", 900L);
        when(authService.refreshAccessToken(any(RefreshRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(refreshRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.id").value(1))
                .andExpect(jsonPath("$.accessToken").value("new-access-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"))
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
    void logout_WithAuthenticatedUser_ShouldReturnNoContent() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout")
                        .principal(new TestingAuthenticationToken("user@example.com", null)))
                .andExpect(status().isNoContent());

        verify(authService).logoutCurrentUser("user@example.com");
    }

    @Test
    void logout_WhenUserCannotBeResolved_ShouldReturnUnauthorized() throws Exception {
        org.mockito.Mockito.doThrow(new InvalidTokenException("Refresh token not found"))
                .when(authService).logoutCurrentUser("user@example.com");

        mockMvc.perform(post("/api/v1/auth/logout")
                        .principal(new TestingAuthenticationToken("user@example.com", null)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Refresh token not found"));
    }

    private RegisterRequest registerRequest() {
        var request = new RegisterRequest();
        request.setEmail("user@example.com");
        request.setPassword("strong-password");
        request.setFullName("User Name");
        request.setRole(Role.STUDENT);
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

    private AuthResponse authResponse(User user) {
        return new AuthResponse(CurrentUser.from(user), "access-token", "refresh-token", "Bearer", 900L);
    }
}
