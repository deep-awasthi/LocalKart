package com.localkart.platform.auth.service;

import com.localkart.platform.auth.client.UserClient;
import com.localkart.platform.auth.domain.UserCredentials;
import com.localkart.platform.auth.repository.UserCredentialsRepository;
import com.localkart.platform.auth.service.impl.AuthServiceImpl;
import com.localkart.platform.auth.web.dto.AuthResponse;
import com.localkart.platform.auth.web.dto.LoginRequest;
import com.localkart.platform.auth.web.dto.RegisterRequest;
import com.localkart.platform.shared.exception.BusinessException;
import com.localkart.platform.shared.exception.ErrorCode;
import com.localkart.platform.shared.security.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTests {

    @Mock
    private UserCredentialsRepository repository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private UserClient userClient;

    @InjectMocks
    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "jwtExpiration", 86400000L);
    }

    @Test
    void testRegisterSuccess() {
        RegisterRequest request = RegisterRequest.builder()
                .email("test@example.com")
                .password("password123")
                .name("Test User")
                .phone("1234567890")
                .roles("ROLE_USER")
                .build();

        when(repository.existsByEmail(request.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(request.getPassword())).thenReturn("encodedPassword");
        when(jwtUtils.generateToken(eq(request.getEmail()), any())).thenReturn("mockToken");

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals("test@example.com", response.getEmail());
        assertEquals("mockToken", response.getToken());
        assertEquals("ROLE_USER", response.getRoles());

        verify(repository, times(1)).save(any(UserCredentials.class));
        verify(userClient, times(1)).createUserProfile(any());
    }

    @Test
    void testRegisterEmailConflict() {
        RegisterRequest request = RegisterRequest.builder()
                .email("duplicate@example.com")
                .password("password123")
                .build();

        when(repository.existsByEmail(request.getEmail())).thenReturn(true);

        BusinessException exception = assertThrows(BusinessException.class, () -> authService.register(request));
        assertEquals(ErrorCode.CONFLICT, exception.getErrorCode());
        verify(repository, never()).save(any());
    }

    @Test
    void testLoginSuccess() {
        LoginRequest request = LoginRequest.builder()
                .email("user@example.com")
                .password("password123")
                .build();

        UserCredentials credentials = UserCredentials.builder()
                .email("user@example.com")
                .password("encodedPassword")
                .roles("ROLE_USER")
                .build();

        when(repository.findByEmail(request.getEmail())).thenReturn(Optional.of(credentials));
        when(passwordEncoder.matches(request.getPassword(), credentials.getPassword())).thenReturn(true);
        when(jwtUtils.generateToken(eq(request.getEmail()), any())).thenReturn("mockToken");

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("mockToken", response.getToken());
        assertEquals("ROLE_USER", response.getRoles());
    }

    @Test
    void testLoginInvalidCredentials() {
        LoginRequest request = LoginRequest.builder()
                .email("user@example.com")
                .password("wrongpassword")
                .build();

        UserCredentials credentials = UserCredentials.builder()
                .email("user@example.com")
                .password("encodedPassword")
                .roles("ROLE_USER")
                .build();

        when(repository.findByEmail(request.getEmail())).thenReturn(Optional.of(credentials));
        when(passwordEncoder.matches(request.getPassword(), credentials.getPassword())).thenReturn(false);

        BusinessException exception = assertThrows(BusinessException.class, () -> authService.login(request));
        assertEquals(ErrorCode.UNAUTHORIZED, exception.getErrorCode());
    }
}
