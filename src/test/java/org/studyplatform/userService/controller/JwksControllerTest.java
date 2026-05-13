package org.studyplatform.userService.controller;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.studyplatform.userService.security.JwtUtil;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.hasKey;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class JwksControllerTest {
    @Test
    void getJwks_ShouldReturnPublicRsaKeyFieldsOnly() throws Exception {
        JwtUtil jwtUtil = mock(JwtUtil.class);
        when(jwtUtil.getJwks()).thenReturn(Map.of(
                "keys", List.of(Map.of(
                        "kty", "RSA",
                        "use", "sig",
                        "kid", "user-service-rsa-1",
                        "alg", "RS256",
                        "n", "public-modulus",
                        "e", "AQAB"
                ))
        ));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new JwksController(jwtUtil)).build();

        mockMvc.perform(get("/api/v1/auth/.well-known/jwks.json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.keys[0].kty").value("RSA"))
                .andExpect(jsonPath("$.keys[0].use").value("sig"))
                .andExpect(jsonPath("$.keys[0].kid").value("user-service-rsa-1"))
                .andExpect(jsonPath("$.keys[0].alg").value("RS256"))
                .andExpect(jsonPath("$.keys[0].n").value("public-modulus"))
                .andExpect(jsonPath("$.keys[0].e").value("AQAB"))
                .andExpect(jsonPath("$.keys[0]", not(hasKey("d"))))
                .andExpect(jsonPath("$.keys[0]", not(hasKey("p"))))
                .andExpect(jsonPath("$.keys[0]", not(hasKey("q"))));
    }
}
