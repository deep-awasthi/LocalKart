package com.localkart.platform.auth.service.impl;

import com.localkart.platform.auth.client.UserClient;
import com.localkart.platform.auth.domain.UserCredentials;
import com.localkart.platform.auth.repository.UserCredentialsRepository;
import com.localkart.platform.auth.service.AuthService;
import com.localkart.platform.auth.web.dto.AuthResponse;
import com.localkart.platform.auth.web.dto.LoginRequest;
import com.localkart.platform.auth.web.dto.RegisterRequest;
import com.localkart.platform.auth.web.dto.UserDto;
import com.localkart.platform.shared.exception.BusinessException;
import com.localkart.platform.shared.exception.ErrorCode;
import com.localkart.platform.shared.security.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserCredentialsRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final UserClient userClient;

    @Value("${jwt.expiration:86400000}")
    private long jwtExpiration;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("Registering user credentials for email: {}", request.getEmail());
        if (repository.existsByEmail(request.getEmail())) {
            throw new BusinessException("Email already in use", ErrorCode.CONFLICT);
        }

        String assignedRoles = request.getRoles() != null && !request.getRoles().isBlank() 
                ? request.getRoles() : "ROLE_USER";

        // 1. Save local login credentials in PostgreSQL
        UserCredentials credentials = UserCredentials.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .roles(assignedRoles)
                .build();
        repository.save(credentials);

        // 2. Propagate profile information to user-service via OpenFeign
        try {
            UserDto profilePayload = UserDto.builder()
                    .email(request.getEmail())
                    .name(request.getName())
                    .phone(request.getPhone())
                    .roles(assignedRoles)
                    .build();
            userClient.createUserProfile(profilePayload);
        } catch (Exception e) {
            log.error("Failed to propagate profile information to user-service for user {}: {}", 
                    request.getEmail(), e.getMessage());
            // In a production app, we would publish an event or compensation transaction.
            // For now, we log the failure and let the registration finish.
        }

        // 3. Generate token
        String token = generateUserToken(request.getEmail(), assignedRoles);

        return AuthResponse.builder()
                .token(token)
                .email(request.getEmail())
                .roles(assignedRoles)
                .expiresIn(jwtExpiration)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        log.info("Authenticating login request for email: {}", request.getEmail());
        UserCredentials credentials = repository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException("Invalid email or password", ErrorCode.UNAUTHORIZED));

        if (!passwordEncoder.matches(request.getPassword(), credentials.getPassword())) {
            throw new BusinessException("Invalid email or password", ErrorCode.UNAUTHORIZED);
        }

        String token = generateUserToken(credentials.getEmail(), credentials.getRoles());

        return AuthResponse.builder()
                .token(token)
                .email(credentials.getEmail())
                .roles(credentials.getRoles())
                .expiresIn(jwtExpiration)
                .build();
    }

    @Override
    public boolean validateToken(String token) {
        try {
            String username = jwtUtils.extractUsername(token);
            return username != null && !jwtUtils.isTokenExpired(token);
        } catch (Exception e) {
            log.warn("Token validation failed dynamically: {}", e.getMessage());
            return false;
        }
    }

    private String generateUserToken(String email, String roles) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", roles);
        return jwtUtils.generateToken(email, claims);
    }
}
