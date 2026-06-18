package com.terimbere.budget.service;

import com.terimbere.budget.dto.request.LoginRequest;
import com.terimbere.budget.dto.request.RegisterRequest;
import com.terimbere.budget.dto.request.TokenRefreshRequest;
import com.terimbere.budget.dto.response.AuthResponse;
import com.terimbere.budget.model.RefreshToken;
import com.terimbere.budget.model.User;
import com.terimbere.budget.repository.RefreshTokenRepository;
import com.terimbere.budget.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class AuthServiceTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    // -------------------------------------------------------------------------
    // Register
    // -------------------------------------------------------------------------

    @Test
    void registerUser_success_createsUserWithDefaults() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("alice@terimbere.com");
        request.setPassword("password123");
        request.setFullName("Alice Mutoni");

        User saved = authService.registerUser(request);

        assertNotNull(saved.getId());
        assertEquals("alice@terimbere.com", saved.getEmail());
        assertEquals("Alice Mutoni", saved.getFullName());
        // Defaults applied when not provided
        assertEquals("RWF", saved.getCurrencyCode());
        assertEquals("Africa/Kigali", saved.getTimezone());
        // NotificationSettings should be initialised
        assertNotNull(saved.getNotificationSettings());
        assertTrue(saved.getNotificationSettings().getEmailNotifications());
        assertTrue(saved.getNotificationSettings().getInAppNotifications());
    }

    @Test
    void registerUser_withCustomCurrencyAndTimezone_savedCorrectly() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("bob@terimbere.com");
        request.setPassword("secret99");
        request.setFullName("Bob Nkurunziza");
        request.setCurrencyCode("USD");
        request.setTimezone("America/New_York");

        User saved = authService.registerUser(request);

        assertEquals("USD", saved.getCurrencyCode());
        assertEquals("America/New_York", saved.getTimezone());
    }

    @Test
    void registerUser_duplicateEmail_throwsIllegalArgumentException() {
        RegisterRequest first = new RegisterRequest();
        first.setEmail("duplicate@terimbere.com");
        first.setPassword("pass1234");
        first.setFullName("First User");
        authService.registerUser(first);

        RegisterRequest second = new RegisterRequest();
        second.setEmail("duplicate@terimbere.com");
        second.setPassword("anotherpass");
        second.setFullName("Second User");

        assertThrows(IllegalArgumentException.class, () -> authService.registerUser(second));
    }

    @Test
    void registerUser_passwordIsHashed_notStoredInPlainText() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("charlie@terimbere.com");
        request.setPassword("plainPassword");
        request.setFullName("Charlie Test");

        User saved = authService.registerUser(request);

        assertNotEquals("plainPassword", saved.getPasswordHash());
        assertTrue(saved.getPasswordHash().startsWith("$2a$") || saved.getPasswordHash().startsWith("$2b$"),
                "Password should be BCrypt-encoded");
    }

    // -------------------------------------------------------------------------
    // Login / Authenticate
    // -------------------------------------------------------------------------

    @Test
    void authenticateUser_validCredentials_returnsAuthResponse() {
        // First register
        RegisterRequest reg = new RegisterRequest();
        reg.setEmail("dana@terimbere.com");
        reg.setPassword("loginPass1");
        reg.setFullName("Dana K");
        authService.registerUser(reg);

        LoginRequest login = new LoginRequest();
        login.setEmail("dana@terimbere.com");
        login.setPassword("loginPass1");

        AuthResponse response = authService.authenticateUser(login);

        assertNotNull(response.getAccessToken());
        assertNotNull(response.getRefreshToken());
        assertEquals("dana@terimbere.com", response.getEmail());
        assertEquals("Dana K", response.getFullName());
    }

    @Test
    void authenticateUser_wrongPassword_throwsBadCredentialsException() {
        RegisterRequest reg = new RegisterRequest();
        reg.setEmail("evan@terimbere.com");
        reg.setPassword("correctPass");
        reg.setFullName("Evan Test");
        authService.registerUser(reg);

        LoginRequest login = new LoginRequest();
        login.setEmail("evan@terimbere.com");
        login.setPassword("wrongPass");

        assertThrows(BadCredentialsException.class, () -> authService.authenticateUser(login));
    }

    @Test
    void authenticateUser_createsRefreshToken_persistedInDatabase() {
        RegisterRequest reg = new RegisterRequest();
        reg.setEmail("fiona@terimbere.com");
        reg.setPassword("fPass1234");
        reg.setFullName("Fiona Test");
        authService.registerUser(reg);

        LoginRequest login = new LoginRequest();
        login.setEmail("fiona@terimbere.com");
        login.setPassword("fPass1234");

        AuthResponse response = authService.authenticateUser(login);

        assertTrue(refreshTokenRepository.findByToken(response.getRefreshToken()).isPresent());
    }

    @Test
    void authenticateUser_secondLogin_replacesOldRefreshToken() {
        RegisterRequest reg = new RegisterRequest();
        reg.setEmail("grace@terimbere.com");
        reg.setPassword("gracePass");
        reg.setFullName("Grace Test");
        authService.registerUser(reg);

        LoginRequest login = new LoginRequest();
        login.setEmail("grace@terimbere.com");
        login.setPassword("gracePass");

        AuthResponse first = authService.authenticateUser(login);
        AuthResponse second = authService.authenticateUser(login);

        // The first refresh token should no longer be in the DB
        assertFalse(refreshTokenRepository.findByToken(first.getRefreshToken()).isPresent(),
                "Old refresh token should have been deleted on second login");
        assertTrue(refreshTokenRepository.findByToken(second.getRefreshToken()).isPresent());
    }

    // -------------------------------------------------------------------------
    // Refresh Token
    // -------------------------------------------------------------------------

    @Test
    void refreshAccessToken_validToken_returnsNewAccessToken() {
        RegisterRequest reg = new RegisterRequest();
        reg.setEmail("harry@terimbere.com");
        reg.setPassword("harryPass");
        reg.setFullName("Harry Test");
        authService.registerUser(reg);

        LoginRequest login = new LoginRequest();
        login.setEmail("harry@terimbere.com");
        login.setPassword("harryPass");
        AuthResponse loginResponse = authService.authenticateUser(login);

        TokenRefreshRequest refreshRequest = new TokenRefreshRequest();
        refreshRequest.setRefreshToken(loginResponse.getRefreshToken());

        AuthResponse refreshed = authService.refreshAccessToken(refreshRequest);

        assertNotNull(refreshed.getAccessToken());
        assertEquals("harry@terimbere.com", refreshed.getEmail());
    }

    @Test
    void refreshAccessToken_invalidToken_throwsIllegalArgumentException() {
        TokenRefreshRequest request = new TokenRefreshRequest();
        request.setRefreshToken(UUID.randomUUID().toString()); // random non-existent token

        assertThrows(IllegalArgumentException.class, () -> authService.refreshAccessToken(request));
    }

    @Test
    void refreshAccessToken_expiredToken_throwsIllegalArgumentException() {
        // Manually insert an expired refresh token
        RegisterRequest reg = new RegisterRequest();
        reg.setEmail("iris@terimbere.com");
        reg.setPassword("irisPass");
        reg.setFullName("Iris Test");
        User user = authService.registerUser(reg);

        RefreshToken expired = RefreshToken.builder()
                .user(user)
                .token(UUID.randomUUID().toString())
                .expiresAt(LocalDateTime.now().minusDays(1)) // already expired
                .revoked(false)
                .build();
        refreshTokenRepository.save(expired);

        TokenRefreshRequest request = new TokenRefreshRequest();
        request.setRefreshToken(expired.getToken());

        assertThrows(IllegalArgumentException.class, () -> authService.refreshAccessToken(request));
    }

    @Test
    void refreshAccessToken_revokedToken_throwsIllegalArgumentException() {
        RegisterRequest reg = new RegisterRequest();
        reg.setEmail("jake@terimbere.com");
        reg.setPassword("jakePass");
        reg.setFullName("Jake Test");
        User user = authService.registerUser(reg);

        RefreshToken revoked = RefreshToken.builder()
                .user(user)
                .token(UUID.randomUUID().toString())
                .expiresAt(LocalDateTime.now().plusWeeks(1))
                .revoked(true)
                .build();
        refreshTokenRepository.save(revoked);

        TokenRefreshRequest request = new TokenRefreshRequest();
        request.setRefreshToken(revoked.getToken());

        assertThrows(IllegalArgumentException.class, () -> authService.refreshAccessToken(request));
    }
}
