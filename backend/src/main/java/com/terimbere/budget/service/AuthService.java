package com.terimbere.budget.service;

import com.terimbere.budget.dto.request.LoginRequest;
import com.terimbere.budget.dto.request.RegisterRequest;
import com.terimbere.budget.dto.request.TokenRefreshRequest;
import com.terimbere.budget.dto.response.AuthResponse;
import com.terimbere.budget.exception.ResourceNotFoundException;
import com.terimbere.budget.model.NotificationSettings;
import com.terimbere.budget.model.RefreshToken;
import com.terimbere.budget.model.User;
import com.terimbere.budget.repository.NotificationSettingsRepository;
import com.terimbere.budget.repository.RefreshTokenRepository;
import com.terimbere.budget.repository.UserRepository;
import com.terimbere.budget.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final AuthenticationManager authenticationManager;

    @Value("${app.jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

    public AuthService(UserRepository userRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider tokenProvider,
                       AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.authenticationManager = authenticationManager;
    }

    @Transactional
    public User registerUser(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email is already registered!");
        }

        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .currencyCode(request.getCurrencyCode() != null ? request.getCurrencyCode() : "RWF")
                .timezone(request.getTimezone() != null ? request.getTimezone() : "Africa/Kigali")
                .build();

        NotificationSettings settings = NotificationSettings.builder()
                .user(user)
                .emailNotifications(true)
                .inAppNotifications(true)
                .daysBeforeBillReminder(3)
                .daysBeforeContractExpiry(7)
                .build();
        user.setNotificationSettings(settings);

        return userRepository.save(user);
    }

    @Transactional
    public AuthResponse authenticateUser(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String jwt = tokenProvider.generateToken(user.getEmail());
        
        // Remove existing refresh tokens before generating a new one
        refreshTokenRepository.deleteByUser(user);
        RefreshToken refreshToken = createRefreshToken(user);

        return AuthResponse.builder()
                .accessToken(jwt)
                .refreshToken(refreshToken.getToken())
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .currencyCode(user.getCurrencyCode())
                .timezone(user.getTimezone())
                .build();
    }

    @Transactional
    public AuthResponse refreshAccessToken(TokenRefreshRequest request) {
        String tokenStr = request.getRefreshToken();
        RefreshToken refreshToken = refreshTokenRepository.findByToken(tokenStr)
                .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));

        if (refreshToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            refreshTokenRepository.delete(refreshToken);
            throw new IllegalArgumentException("Refresh token has expired. Please log in again.");
        }

        if (refreshToken.getRevoked()) {
            throw new IllegalArgumentException("Refresh token was revoked.");
        }

        User user = refreshToken.getUser();
        String jwt = tokenProvider.generateToken(user.getEmail());

        return AuthResponse.builder()
                .accessToken(jwt)
                .refreshToken(refreshToken.getToken())
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .currencyCode(user.getCurrencyCode())
                .timezone(user.getTimezone())
                .build();
    }

    private RefreshToken createRefreshToken(User user) {
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(UUID.randomUUID().toString())
                .expiresAt(LocalDateTime.now().plusWeeks(1)) // 1 week
                .revoked(false)
                .build();

        return refreshTokenRepository.save(refreshToken);
    }

    public User getCurrentAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("User is not authenticated");
        }
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }
}
