package com.resqcloud.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resqcloud.user.dto.LoginRequest;
import com.resqcloud.user.dto.RegisterRequest;
import com.resqcloud.user.entity.RoleType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Should successfully authenticate seeded admin user and return JWT")
    void testLoginSuccess() throws Exception {
        LoginRequest loginRequest = new LoginRequest("admin", "Password123!");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").value(notNullValue()))
                .andExpect(jsonPath("$.data.username").value("admin"))
                .andExpect(jsonPath("$.data.role").value("ROLE_ADMIN"));
    }

    @Test
    @DisplayName("Should reject invalid login credentials")
    void testLoginFailure() throws Exception {
        LoginRequest loginRequest = new LoginRequest("admin", "WrongPassword!");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("Should register a new citizen user successfully")
    void testRegisterNewCitizen() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("testcitizen_" + System.currentTimeMillis());
        request.setEmail("citizen_" + System.currentTimeMillis() + "@test.com");
        request.setPassword("SecurePass123!");
        request.setFullName("Jane Citizen");
        request.setPhoneNumber("+1-555-0199");
        request.setRole(RoleType.ROLE_CITIZEN);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value(request.getUsername()))
                .andExpect(jsonPath("$.data.role").value("ROLE_CITIZEN"));
    }
}
