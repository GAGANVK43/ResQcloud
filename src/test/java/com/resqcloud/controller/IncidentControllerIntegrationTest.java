package com.resqcloud.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resqcloud.incident.dto.IncidentCreateRequest;
import com.resqcloud.incident.entity.IncidentSeverity;
import com.resqcloud.incident.entity.IncidentType;
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

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class IncidentControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String citizenToken;

    @BeforeEach
    void setUp() throws Exception {
        LoginRequest login = new LoginRequest("citizen_john", "Password123!");
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        citizenToken = objectMapper.readTree(responseBody).path("data").path("token").asText();
    }

    @Test
    @DisplayName("Should reject unauthenticated access to incident endpoints")
    void testUnauthenticatedAccessRejected() throws Exception {
        mockMvc.perform(get("/api/incidents"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Citizen should report an emergency and receive an incident ID with auto-calculated priority")
    void testReportIncidentWithPriorityCalculated() throws Exception {
        IncidentCreateRequest request = new IncidentCreateRequest(
                "Severe Warehouse Structural Collapse",
                "Roof collapsed under load. Multiple warehouse workers trapped inside.",
                IncidentType.BUILDING_EMERGENCY,
                IncidentSeverity.CRITICAL,
                37.7749,
                -122.4194,
                "900 Folsom St, SOMA, San Francisco, CA",
                6
        );

        mockMvc.perform(post("/api/incidents")
                        .header("Authorization", "Bearer " + citizenToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.incidentNumber").value(notNullValue()))
                .andExpect(jsonPath("$.data.status").value("REPORTED"))
                .andExpect(jsonPath("$.data.priorityScore").value(greaterThan(60.0)))
                .andExpect(jsonPath("$.data.priorityLevel").value(notNullValue()));
    }

    @Test
    @DisplayName("Should query and filter incidents with pagination")
    void testQueryIncidentsWithFilter() throws Exception {
        mockMvc.perform(get("/api/incidents")
                        .header("Authorization", "Bearer " + citizenToken)
                        .param("page", "0")
                        .param("size", "10")
                        .param("type", "ACCIDENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray());
    }
}
