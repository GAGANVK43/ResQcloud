package com.resqcloud.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resqcloud.incident.entity.Incident;
import com.resqcloud.incident.repository.IncidentRepository;
import com.resqcloud.user.dto.LoginRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ResourceControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private IncidentRepository incidentRepository;

    private String coordinatorToken;

    @BeforeEach
    void setUp() throws Exception {
        LoginRequest login = new LoginRequest("coordinator", "Password123!");
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        coordinatorToken = objectMapper.readTree(responseBody).path("data").path("token").asText();
    }

    @Test
    @DisplayName("Should query available resources successfully")
    void testGetAvailableResources() throws Exception {
        mockMvc.perform(get("/api/resources/available")
                        .header("Authorization", "Bearer " + coordinatorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(greaterThanOrEqualTo(1)));
    }

    @Test
    @DisplayName("Coordinator should get intelligent resource match recommendations for an incident")
    void testGetResourceRecommendations() throws Exception {
        Incident incident = incidentRepository.findAll().stream().findFirst().orElseThrow();

        mockMvc.perform(get("/api/resources/matching/" + incident.getId())
                        .header("Authorization", "Bearer " + coordinatorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].matchScore").value(notNullValue()))
                .andExpect(jsonPath("$.data[0].distanceKm").value(notNullValue()));
    }
}
