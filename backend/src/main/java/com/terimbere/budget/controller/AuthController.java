package com.terimbere.budget.controller;

import com.terimbere.budget.dto.request.LoginRequest;
import com.terimbere.budget.dto.request.RegisterRequest;
import com.terimbere.budget.dto.request.TokenRefreshRequest;
import com.terimbere.budget.dto.response.AuthResponse;
import com.terimbere.budget.model.User;
import com.terimbere.budget.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication Center", description = "Endpoints for registering profiles, logging in, and refreshing JWT tokens.")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @Operation(summary = "Register a new user profile", description = "Creates a brand new user profile, sets up timezone, and initializes automatic notification settings.")
    public ResponseEntity<User> registerUser(@Valid @RequestBody RegisterRequest request) {
        User user = authService.registerUser(request);
        return new ResponseEntity<>(user, HttpStatus.CREATED);
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate user credentials", description = "Validates user email and password, creates secure stateless access JWT and long-lived refresh token.")
    public ResponseEntity<AuthResponse> authenticateUser(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.authenticateUser(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh stateless JWT access token", description = "Takes a valid, non-revoked refresh token to issue a fresh JWT access token.")
    public ResponseEntity<AuthResponse> refreshAccessToken(@Valid @RequestBody TokenRefreshRequest request) {
        AuthResponse response = authService.refreshAccessToken(request);
        return ResponseEntity.ok(response);
    }
}
