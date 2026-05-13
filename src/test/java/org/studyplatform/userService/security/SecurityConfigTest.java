package org.studyplatform.userService.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SecurityConfigTest {
    @Test
    void passwordEncoder_ShouldUseBCrypt() {
        var config = new SecurityConfig(mock(JwtUtil.class), mock(CustomUserDetailsService.class));

        assertInstanceOf(BCryptPasswordEncoder.class, config.passwordEncoder());
    }

    @Test
    void authenticationManager_ShouldComeFromAuthenticationConfiguration() throws Exception {
        var expectedManager = mock(AuthenticationManager.class);
        var authenticationConfiguration = mock(AuthenticationConfiguration.class);
        when(authenticationConfiguration.getAuthenticationManager()).thenReturn(expectedManager);
        var config = new SecurityConfig(mock(JwtUtil.class), mock(CustomUserDetailsService.class));

        var actualManager = config.authenticationManager(authenticationConfiguration);

        assertSame(expectedManager, actualManager);
    }
}
