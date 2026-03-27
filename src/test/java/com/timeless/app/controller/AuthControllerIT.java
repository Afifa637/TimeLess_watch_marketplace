package com.timeless.app.controller;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.timeless.app.entity.Role;
import com.timeless.app.entity.UserAccount;
import com.timeless.app.repository.UserAccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthControllerIT {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UserAccountRepository userAccountRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void register_validBuyer_returns201WithToken() throws Exception {
        String body = """
            {
              "email":"buyer-it@test.com",
              "password":"Buyer123!",
              "fullName":"Buyer IT",
              "role":"BUYER"
            }
            """;

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.token", notNullValue()))
            .andExpect(jsonPath("$.role").value("BUYER"));
    }

    @Test
    void register_duplicateEmail_returns400() throws Exception {
        userAccountRepository.save(UserAccount.builder()
            .email("duplicate@test.com")
            .passwordHash(passwordEncoder.encode("Secret123!"))
            .fullName("Duplicate")
            .role(Role.BUYER)
            .enabled(true)
            .build());

        String body = """
            {
              "email":"duplicate@test.com",
              "password":"Buyer123!",
              "fullName":"Buyer IT",
              "role":"BUYER"
            }
            """;

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest());
    }

    @Test
    void register_adminRole_returns400() throws Exception {
        String body = """
            {
              "email":"admin-register@test.com",
              "password":"Admin123!",
              "fullName":"Admin Register",
              "role":"ADMIN"
            }
            """;

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest());
    }

    @Test
    void login_validCredentials_returns200WithToken() throws Exception {
        userAccountRepository.save(UserAccount.builder()
            .email("login@test.com")
            .passwordHash(passwordEncoder.encode("Buyer123!"))
            .fullName("Login User")
            .role(Role.BUYER)
            .enabled(true)
            .build());

        String body = """
            {
              "email":"login@test.com",
              "password":"Buyer123!"
            }
            """;

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token", notNullValue()))
            .andExpect(jsonPath("$.email").value("login@test.com"));
    }

    @Test
    void login_wrongPassword_returns401() throws Exception {
        userAccountRepository.save(UserAccount.builder()
            .email("login-wrong@test.com")
            .passwordHash(passwordEncoder.encode("Buyer123!"))
            .fullName("Wrong Password User")
            .role(Role.BUYER)
            .enabled(true)
            .build());

        String body = """
            {
              "email":"login-wrong@test.com",
              "password":"Wrong123!"
            }
            """;

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isUnauthorized());
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
}
