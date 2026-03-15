package com.timeless.app.service;

import com.timeless.app.dto.request.LoginRequest;
import com.timeless.app.dto.request.RegisterRequest;
import com.timeless.app.dto.response.AuthResponse;
import com.timeless.app.entity.Role;
import com.timeless.app.entity.UserAccount;
import com.timeless.app.exception.BadRequestException;
import com.timeless.app.exception.ResourceNotFoundException;
import com.timeless.app.repository.UserAccountRepository;
import com.timeless.app.security.JwtService;
import com.timeless.app.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        String email = normalizeEmail(req.getEmail());
        if (userAccountRepository.existsByEmail(email)) {
            throw new BadRequestException("Email is already registered");
        }

        Role role = parseRegisterRole(req.getRole());
        UserAccount user = UserAccount.builder()
            .email(email)
            .passwordHash(passwordEncoder.encode(req.getPassword()))
            .fullName(req.getFullName().trim())
            .phone(blankToNull(req.getPhone()))
            .address(blankToNull(req.getAddress()))
            .role(role)
            .enabled(true)
            .emailVerified(false)
            .build();

        UserAccount saved = userAccountRepository.save(user);
        UserPrincipal principal = new UserPrincipal(saved);
        return AuthResponse.builder()
            .token(jwtService.generateToken(principal))
            .email(saved.getEmail())
            .fullName(saved.getFullName())
            .role(saved.getRole().name())
            .build();
    }

    public AuthResponse login(LoginRequest req) {
        String email = normalizeEmail(req.getEmail());
        UserAccount user = userAccountRepository.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!user.isEnabled()) {
            throw new BadRequestException("Account is disabled");
        }
        if (!passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        UserPrincipal principal = new UserPrincipal(user);
        return AuthResponse.builder()
            .token(jwtService.generateToken(principal))
            .email(user.getEmail())
            .fullName(user.getFullName())
            .role(user.getRole().name())
            .build();
    }

    private Role parseRegisterRole(String roleValue) {
        if (roleValue == null || roleValue.isBlank()) {
            throw new BadRequestException("Role is required");
        }
        try {
            Role role = Role.valueOf(roleValue.trim().toUpperCase());
            if (role == Role.ADMIN) {
                throw new BadRequestException("ADMIN role cannot self-register");
            }
            if (role != Role.SELLER && role != Role.BUYER) {
                throw new BadRequestException("Invalid role");
            }
            return role;
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Invalid role");
        }
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
