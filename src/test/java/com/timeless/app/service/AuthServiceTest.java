package com.timeless.app.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.timeless.app.dto.request.LoginRequest;
import com.timeless.app.dto.request.RegisterRequest;
import com.timeless.app.dto.response.AuthResponse;
import com.timeless.app.entity.Role;
import com.timeless.app.entity.UserAccount;
import com.timeless.app.exception.BadRequestException;
import com.timeless.app.exception.ResourceNotFoundException;
import com.timeless.app.repository.UserAccountRepository;
import com.timeless.app.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserAccountRepository userAccountRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userAccountRepository, passwordEncoder, jwtService);
    }

    @Test
    void register_success_returnAuthResponse() {
        RegisterRequest request = RegisterRequest.builder()
            .email("buyer@test.com")
            .password("secret")
            .fullName("Buyer One")
            .role("BUYER")
            .build();

        UserAccount saved = UserAccount.builder()
            .id(1L)
            .email("buyer@test.com")
            .fullName("Buyer One")
            .role(Role.BUYER)
            .passwordHash("encoded")
            .enabled(true)
            .build();

        when(userAccountRepository.existsByEmail("buyer@test.com")).thenReturn(false);
        when(passwordEncoder.encode("secret")).thenReturn("encoded");
        when(userAccountRepository.save(any(UserAccount.class))).thenReturn(saved);
        when(jwtService.generateToken(any())).thenReturn("jwt-token");

        AuthResponse response = authService.register(request);

        assertThat(response.getToken()).isEqualTo("jwt-token");
        assertThat(response.getEmail()).isEqualTo("buyer@test.com");
        assertThat(response.getRole()).isEqualTo("BUYER");
        verify(userAccountRepository).save(any(UserAccount.class));
    }

    @Test
    void register_emailAlreadyExists_throwsBadRequestException() {
        RegisterRequest request = RegisterRequest.builder()
            .email("duplicate@test.com")
            .password("secret")
            .fullName("Duplicate")
            .role("BUYER")
            .build();

        when(userAccountRepository.existsByEmail("duplicate@test.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
            .isInstanceOf(BadRequestException.class)
            .hasMessageContaining("already registered");
    }

    @Test
    void register_adminRole_throwsBadRequestException() {
        RegisterRequest request = RegisterRequest.builder()
            .email("admin@test.com")
            .password("secret")
            .fullName("Admin")
            .role("ADMIN")
            .build();

        when(userAccountRepository.existsByEmail("admin@test.com")).thenReturn(false);

        assertThatThrownBy(() -> authService.register(request))
            .isInstanceOf(BadRequestException.class)
            .hasMessageContaining("cannot self-register");
    }

    @Test
    void login_validCredentials_returnAuthResponse() {
        LoginRequest request = LoginRequest.builder()
            .email("buyer@test.com")
            .password("secret")
            .build();

        UserAccount user = UserAccount.builder()
            .id(1L)
            .email("buyer@test.com")
            .fullName("Buyer")
            .role(Role.BUYER)
            .passwordHash("encoded")
            .enabled(true)
            .build();

        when(userAccountRepository.findByEmail("buyer@test.com")).thenReturn(java.util.Optional.of(user));
        when(passwordEncoder.matches("secret", "encoded")).thenReturn(true);
        when(jwtService.generateToken(any())).thenReturn("jwt-token");

        AuthResponse response = authService.login(request);

        assertThat(response.getToken()).isEqualTo("jwt-token");
        assertThat(response.getFullName()).isEqualTo("Buyer");
        assertThat(response.getRole()).isEqualTo("BUYER");
    }

    @Test
    void login_wrongPassword_throwsBadCredentialsException() {
        LoginRequest request = LoginRequest.builder()
            .email("buyer@test.com")
            .password("wrong")
            .build();

        UserAccount user = UserAccount.builder()
            .email("buyer@test.com")
            .passwordHash("encoded")
            .role(Role.BUYER)
            .enabled(true)
            .build();

        when(userAccountRepository.findByEmail("buyer@test.com")).thenReturn(java.util.Optional.of(user));
        when(passwordEncoder.matches("wrong", "encoded")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
            .isInstanceOf(BadCredentialsException.class)
            .hasMessageContaining("Invalid email or password");
    }

    @Test
    void login_disabledAccount_throwsBadRequestException() {
        LoginRequest request = LoginRequest.builder()
            .email("buyer@test.com")
            .password("secret")
            .build();

        UserAccount user = UserAccount.builder()
            .email("buyer@test.com")
            .passwordHash("encoded")
            .role(Role.BUYER)
            .enabled(false)
            .build();

        when(userAccountRepository.findByEmail("buyer@test.com")).thenReturn(java.util.Optional.of(user));

        assertThatThrownBy(() -> authService.login(request))
            .isInstanceOf(BadRequestException.class)
            .hasMessageContaining("disabled");
    }
}
