package com.localkart.platform.auth.web.rest;

import com.localkart.platform.auth.service.AuthService;
import com.localkart.platform.auth.web.dto.AuthResponse;
import com.localkart.platform.auth.web.dto.LoginRequest;
import com.localkart.platform.auth.web.dto.RegisterRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Validated
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        log.info("REST request to register user: {}", request.getEmail());
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        log.info("REST request to authenticate user: {}", request.getEmail());
        return authService.login(request);
    }

    @GetMapping("/validate")
    public boolean validateToken(@RequestParam("token") String token) {
        log.info("REST request to validate authorization token");
        return authService.validateToken(token);
    }
}
