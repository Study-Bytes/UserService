package org.studyplatform.userService.controller;

import jakarta.validation.Valid;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.studyplatform.userService.dto.RegisterRequest;
import org.studyplatform.userService.exception.EmailAlreadyTakenException;
import org.studyplatform.userService.exception.InvalidTokenException;
import org.studyplatform.userService.exception.UserNotFoundException;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RestExceptionHandlerTest {
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new ErrorTestController())
                .setControllerAdvice(new RestExceptionHandler())
                .build();
    }

    @Test
    void shouldReturnConflictForDuplicateEmail() throws Exception {
        mockMvc.perform(get("/errors/email-taken"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(409))
                .andExpect(jsonPath("$.message").value("User with email already exists: user@test.com"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void shouldReturnNotFoundForMissingUser() throws Exception {
        mockMvc.perform(get("/errors/user-not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("User not found"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void shouldReturnUnauthorizedForInvalidToken() throws Exception {
        mockMvc.perform(get("/errors/invalid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("Invalid refresh token"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void shouldReturnUnauthorizedForBadCredentials() throws Exception {
        mockMvc.perform(get("/errors/bad-credentials"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("Invalid email or password"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void shouldReturnBadRequestForValidationError() throws Exception {
        mockMvc.perform(post("/errors/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"\",\"password\":\"123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message", containsString("email")))
                .andExpect(jsonPath("$.message", containsString("password")))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void shouldReturnInternalServerErrorForUnexpectedError() throws Exception {
        mockMvc.perform(get("/errors/unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("Internal server error"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @RestController
    private static class ErrorTestController {
        @GetMapping("/errors/email-taken")
        void emailTaken() {
            throw new EmailAlreadyTakenException("user@test.com");
        }

        @GetMapping("/errors/user-not-found")
        void userNotFound() {
            throw new UserNotFoundException("User not found");
        }

        @GetMapping("/errors/invalid-token")
        void invalidToken() {
            throw new InvalidTokenException("Invalid refresh token");
        }

        @GetMapping("/errors/bad-credentials")
        void badCredentials() {
            throw new BadCredentialsException("Bad credentials");
        }

        @PostMapping("/errors/validation")
        void validation(@Valid @RequestBody RegisterRequest request) {
        }

        @GetMapping("/errors/unexpected")
        void unexpected() {
            throw new RuntimeException("Unexpected error");
        }
    }
}
