package com.timeless.app.controller;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
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

        mockMvc.perform(buildCreateWatchRequest(token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.status").value("PENDING_REVIEW"))
                .andExpect(jsonPath("$.name").value("Integration Rolex"));
    }

    @Test
    void createWatch_asBuyer_returns403() throws Exception {
        String token = registerAndGetToken("buyer-watch@test.com", "Buyer123!", "BUYER");

        mockMvc.perform(buildCreateWatchRequest(token))
                .andExpect(status().isForbidden());
    }

    @Test
    void createWatch_unauthenticated_returns401() throws Exception {
        mockMvc.perform(buildCreateWatchRequest(null))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateWatch_sellerUpdatesOwn_returns200() throws Exception {
        String token = registerAndGetToken("seller-update@test.com", "Seller123!", "SELLER");

        MvcResult createResult = mockMvc.perform(buildCreateWatchRequest(token))
                .andExpect(status().isCreated())
                .andReturn();

        Long watchId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("id")
                .asLong();

        Watch watch = watchRepository.findById(watchId).orElseThrow();
        watch.setStatus(WatchStatus.ACTIVE);
        watchRepository.save(watch);

        MockMultipartHttpServletRequestBuilder requestBuilder = multipart("/api/watches/{id}", watchId);
        requestBuilder.with(request -> {
            request.setMethod("PUT");
            return request;
        });
        requestBuilder.with(csrf());
        requestBuilder.file(optionalImageFile());
        requestBuilder.contentType(MediaType.MULTIPART_FORM_DATA);
        requestBuilder.param("name", "Updated Integration Watch");
        requestBuilder.param("brand", "Rolex");
        requestBuilder.param("category", "Luxury");
        requestBuilder.param("condition", "LIKE_NEW");
        requestBuilder.param("price", "6500.00");
        requestBuilder.param("stockQuantity", "1");
        requestBuilder.param("description", "Updated integration test watch");

        mockMvc.perform(requestBuilder.header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Integration Watch"))
                .andExpect(jsonPath("$.status").value("PENDING_REVIEW"));
    }

    private MockMultipartHttpServletRequestBuilder buildCreateWatchRequest(String token) {
        MockMultipartHttpServletRequestBuilder requestBuilder = multipart("/api/watches");
        requestBuilder.with(csrf());
        requestBuilder.file(optionalImageFile());
        requestBuilder.contentType(MediaType.MULTIPART_FORM_DATA);
        requestBuilder.param("name", "Integration Rolex");
        requestBuilder.param("brand", "Rolex");
        requestBuilder.param("category", "Luxury");
        requestBuilder.param("condition", "LIKE_NEW");
        requestBuilder.param("price", "6000.00");
        requestBuilder.param("stockQuantity", "1");
        requestBuilder.param("description", "Integration test watch");

        if (token != null) {
            requestBuilder.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }

        return requestBuilder;
    }

    private MockMultipartFile optionalImageFile() {
        return new MockMultipartFile(
                "imageFile",
                "watch.png",
                MediaType.IMAGE_PNG_VALUE,
                "fake-image-content".getBytes()
        );
    }

    private String registerAndGetToken(String email, String password, String role) throws Exception {
        String body = String.format(
                "{\"email\":\"%s\",\"password\":\"%s\",\"fullName\":\"%s\",\"role\":\"%s\"}",
                email,
                password,
                role + " User",
                role
        );

        MvcResult result = mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/auth/register")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body)
                )
                .andReturn();

        JsonNode jsonNode = objectMapper.readTree(result.getResponse().getContentAsString());
        return jsonNode.get("token").asText();
    }
}