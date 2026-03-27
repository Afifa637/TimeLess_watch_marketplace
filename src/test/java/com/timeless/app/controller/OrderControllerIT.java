package com.timeless.app.controller;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.timeless.app.entity.Role;
import com.timeless.app.entity.UserAccount;
import com.timeless.app.entity.Watch;
import com.timeless.app.entity.WatchStatus;
import com.timeless.app.repository.UserAccountRepository;
import com.timeless.app.repository.WatchRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class OrderControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private WatchRepository watchRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void placeOrder_asBuyer_returns201() throws Exception {
        String sellerToken = registerAndGetToken("seller-order@test.com", "Seller123!", "SELLER");
        Long watchId = createAndActivateWatch(sellerToken);
        String buyerToken = registerAndGetToken("buyer-order@test.com", "Buyer123!", "BUYER");

        mockMvc.perform(post("/api/orders")
                        .with(csrf())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + buyerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"watchId\":" + watchId + "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void placeOrder_asSeller_returns403() throws Exception {
        String sellerToken = registerAndGetToken("seller-place@test.com", "Seller123!", "SELLER");
        Long watchId = createAndActivateWatch(sellerToken);

        mockMvc.perform(post("/api/orders")
                        .with(csrf())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + sellerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"watchId\":" + watchId + "}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getMyOrders_asBuyer_returns200() throws Exception {
        String sellerToken = registerAndGetToken("seller-myorders@test.com", "Seller123!", "SELLER");
        Long watchId = createAndActivateWatch(sellerToken);
        String buyerToken = registerAndGetToken("buyer-myorders@test.com", "Buyer123!", "BUYER");

        mockMvc.perform(post("/api/orders")
                        .with(csrf())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + buyerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"watchId\":" + watchId + "}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/orders/my")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + buyerToken))
                .andExpect(status().isOk());
    }

    @Test
    void getAllOrders_asAdmin_returns200() throws Exception {
        userAccountRepository.save(UserAccount.builder()
                .email("admin@test.com")
                .passwordHash(passwordEncoder.encode("Admin123!"))
                .fullName("Admin")
                .role(Role.ADMIN)
                .enabled(true)
                .build());

        String adminToken = loginAndGetToken("admin@test.com", "Admin123!");

        mockMvc.perform(get("/api/orders")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    void getAllOrders_asBuyer_returns403() throws Exception {
        String buyerToken = registerAndGetToken("buyer-allorders@test.com", "Buyer123!", "BUYER");

        mockMvc.perform(get("/api/orders")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + buyerToken))
                .andExpect(status().isForbidden());
    }

    private Long createAndActivateWatch(String sellerToken) throws Exception {
        MockMultipartHttpServletRequestBuilder requestBuilder = multipart("/api/watches");
        requestBuilder.with(csrf());
        requestBuilder.file(new MockMultipartFile(
                "imageFile",
                "watch.png",
                MediaType.IMAGE_PNG_VALUE,
                "fake-image-content".getBytes()
        ));
        requestBuilder.contentType(MediaType.MULTIPART_FORM_DATA);
        requestBuilder.header(HttpHeaders.AUTHORIZATION, "Bearer " + sellerToken);
        requestBuilder.param("name", "Order Flow Watch");
        requestBuilder.param("brand", "Omega");
        requestBuilder.param("category", "Sport");
        requestBuilder.param("condition", "GOOD");
        requestBuilder.param("price", "5200.00");
        requestBuilder.param("stockQuantity", "3");

        MvcResult result = mockMvc.perform(requestBuilder)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andReturn();

        Long watchId = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("id")
                .asLong();

        Watch watch = watchRepository.findById(watchId).orElseThrow();
        watch.setStatus(WatchStatus.ACTIVE);
        watchRepository.save(watch);
        return watchId;
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
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn();

        JsonNode jsonNode = objectMapper.readTree(result.getResponse().getContentAsString());
        return jsonNode.get("token").asText();
    }

    private String loginAndGetToken(String email, String password) throws Exception {
        String body = String.format("{\"email\":\"%s\",\"password\":\"%s\"}", email, password);

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn();

        JsonNode jsonNode = objectMapper.readTree(result.getResponse().getContentAsString());
        return jsonNode.get("token").asText();
    }
}