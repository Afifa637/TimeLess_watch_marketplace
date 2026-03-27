package com.timeless.app.controller;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.timeless.app.entity.Watch;
import com.timeless.app.entity.WatchStatus;
import com.timeless.app.repository.WatchRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class WatchControllerIT {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private WatchRepository watchRepository;

    @Test
    void getWatches_public_returns200() throws Exception {
        mockMvc.perform(get("/api/watches"))
            .andExpect(status().isOk());
    }

    @Test
    void createWatch_asSeller_returns201WithPendingReviewStatus() throws Exception {
        String token = registerAndGetToken("seller-watch@test.com", "Seller123!", "SELLER");

        mockMvc.perform(post("/api/watches")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(sampleWatchBody()))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id", notNullValue()))
            .andExpect(jsonPath("$.status").value("PENDING_REVIEW"));
    }

    @Test
    void createWatch_asBuyer_returns403() throws Exception {
        String token = registerAndGetToken("buyer-watch@test.com", "Buyer123!", "BUYER");

        mockMvc.perform(post("/api/watches")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(sampleWatchBody()))
            .andExpect(status().isForbidden());
    }

    @Test
    void createWatch_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/api/watches")
                .contentType(MediaType.APPLICATION_JSON)
                .content(sampleWatchBody()))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void updateWatch_sellerUpdatesOwn_returns200() throws Exception {
        String token = registerAndGetToken("seller-update@test.com", "Seller123!", "SELLER");
        MvcResult createResult = mockMvc.perform(post("/api/watches")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(sampleWatchBody()))
            .andExpect(status().isCreated())
            .andReturn();

        Long watchId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();
        Watch watch = watchRepository.findById(watchId).orElseThrow();
        watch.setStatus(WatchStatus.ACTIVE);
        watchRepository.save(watch);

        String updateBody = """
            {
              "name":"Updated Integration Watch",
              "price":6500.00
            }
            """;

        mockMvc.perform(put("/api/watches/{id}", watchId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Updated Integration Watch"))
            .andExpect(jsonPath("$.status").value("PENDING_REVIEW"));
    }

    private String registerAndGetToken(String email, String password, String role) throws Exception {
        String body = String.format(
            "{\"email\":\"%s\",\"password\":\"%s\",\"fullName\":\"%s\",\"role\":\"%s\"}",
            email,
            password,
            role + " User",
            role
        );
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andReturn();
        JsonNode jsonNode = objectMapper.readTree(result.getResponse().getContentAsString());
        return jsonNode.get("token").asText();
    }

    private String loginAndGetToken(String email, String password) throws Exception {
        String body = String.format("{\"email\":\"%s\",\"password\":\"%s\"}", email, password);
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andReturn();
        JsonNode jsonNode = objectMapper.readTree(result.getResponse().getContentAsString());
        return jsonNode.get("token").asText();
    }

    private String sampleWatchBody() {
        return """
            {
              "name":"Integration Rolex",
              "brand":"Rolex",
              "category":"Luxury",
              "condition":"LIKE_NEW",
              "price":6000.00,
              "stockQuantity":1,
              "description":"Integration test watch"
            }
            """;
    }
}
