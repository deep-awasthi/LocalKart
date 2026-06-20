package com.localkart.platform.auth.service;

import com.localkart.platform.auth.web.dto.AuthResponse;
import com.localkart.platform.auth.web.dto.LoginRequest;
import com.localkart.platform.auth.web.dto.RegisterRequest;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    boolean validateToken(String token);
}
