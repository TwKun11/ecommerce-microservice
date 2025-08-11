package org.kun.userservice.controller;

import org.kun.userservice.security.KeycloakCookieService;
import org.kun.userservice.security.KeycloakTokenService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.util.Map;

/**
 * 🚀 SECURE AUTH CONTROLLER - BIG TECH STANDARD
 * 
 * Authorization Code Flow với Backend-only Token Handling:
 * ✅ Frontend không bao giờ touch refresh token
 * ✅ HttpOnly cookie cho refresh token
 * ✅ Memory-only access token
 * ✅ Token rotation
 */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"}, allowCredentials = "true")
public class SecureAuthController {

    private final KeycloakCookieService cookieService;
    private final KeycloakTokenService tokenService;

    @Value("${keycloak.auth-server-url}")
    private String keycloakUrl;

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.resource}")
    private String clientId;

    @Value("${keycloak.credentials.secret}")
    private String clientSecret;

    @Value("${app.url:http://localhost:3000}")
    private String appUrl;

    public SecureAuthController(KeycloakCookieService cookieService, KeycloakTokenService tokenService) {
        this.cookieService = cookieService;
        this.tokenService = tokenService;
    }

    /**
     * Initiate login - redirect to Keycloak
     */
    @GetMapping("/login")
    public void initiateLogin(
            @RequestParam String redirect_uri,
            HttpServletResponse response) throws Exception {
        
        String state = generateState();
        // Store redirect_uri in session or use it directly
        String authUrl = buildAuthUrl(redirect_uri, state);
        
        response.sendRedirect(authUrl);
    }

    /**
     * Handle Keycloak callback - exchange code for tokens
     */
    @GetMapping("/callback")
    public ResponseEntity<?> handleCallback(
            @RequestParam String code,
            @RequestParam String state,
            HttpServletResponse response) throws Exception {
        
        try {
            // 1. Exchange authorization code for tokens
            KeycloakTokenService.TokenResponse tokens = tokenService.exchangeCodeForTokens(code);
            
            // 2. Store refresh token in HttpOnly cookie
            ResponseCookie refreshCookie = cookieService.createRefreshTokenCookie(tokens.getRefreshToken());
            
            // 3. Redirect về frontend với access token trong URL fragment
            String redirectUrl = "http://localhost:5173/#access_token=" + tokens.getAccessToken() + 
                               "&expires_in=" + tokens.getExpiresIn() + 
                               "&token_type=Bearer";
            
            return ResponseEntity.status(302)
                    .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                    .header(HttpHeaders.LOCATION, redirectUrl)
                    .build();
                    
        } catch (Exception e) {
            // Redirect to error page
            String errorUrl = "http://localhost:5173/#error=authentication_failed&error_description=" + 
                            encodeURIComponent(e.getMessage());
            
            return ResponseEntity.status(302)
                    .header(HttpHeaders.LOCATION, errorUrl)
                    .build();
        }
    }

    /**
     * Refresh tokens - backend only
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshTokens(HttpServletRequest request) {
        try {
            // 1. Extract refresh token from HttpOnly cookie
            String refreshToken = extractRefreshTokenFromCookie(request);
            
            if (refreshToken == null) {
                return ResponseEntity.status(401)
                        .body(Map.of("error", "No refresh token found"));
            }
            
            // 2. Call Keycloak refresh endpoint
            KeycloakTokenService.TokenResponse newTokens = tokenService.refreshTokens(refreshToken);
            
            // 3. Update HttpOnly cookie với new refresh token (rotation)
            ResponseCookie newRefreshCookie = cookieService.createRefreshTokenCookie(newTokens.getRefreshToken());
            
            // 4. Return new access token only
            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, newRefreshCookie.toString())
                    .body(Map.of(
                        "access_token", newTokens.getAccessToken(),
                        "expires_in", newTokens.getExpiresIn(),
                        "token_type", "Bearer"
                    ));
                    
        } catch (Exception e) {
            // Refresh token expired/invalid
            ResponseCookie clearCookie = cookieService.clearRefreshTokenCookie();
            
            return ResponseEntity.status(401)
                    .header(HttpHeaders.SET_COOKIE, clearCookie.toString())
                    .body(Map.of("error", "Refresh token expired"));
        }
    }

    /**
     * Logout - clear cookies
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        // Clear HttpOnly cookie
        ResponseCookie clearCookie = cookieService.clearRefreshTokenCookie();
        
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, clearCookie.toString())
                .body(Map.of("message", "Logged out successfully"));
    }

    /**
     * Redirect to Keycloak logout
     */
    @GetMapping("/logout-redirect")
    public void logoutRedirect(HttpServletResponse response) throws Exception {
        String keycloakLogoutUrl = buildLogoutUrl();
        response.sendRedirect(keycloakLogoutUrl);
    }

    /**
     * Health check
     */
    @GetMapping("/health")
    public ResponseEntity<?> health() {
        return ResponseEntity.ok(Map.of(
            "status", "healthy",
            "service", "SecureAuthController",
            "flow", "Authorization Code",
            "standard", "Big Tech"
        ));
    }

    // Private helper methods
    private String buildAuthUrl(String redirectUri, String state) {
        return String.format("%s/realms/%s/protocol/openid-connect/auth" +
                "?client_id=%s" +
                "&response_type=code" +
                "&scope=openid profile email" +
                "&redirect_uri=%s" +
                "&state=%s",
                keycloakUrl, realm, clientId, 
                encodeURIComponent("http://localhost:8083/api/auth/callback"), 
                state);
    }

    private String buildLogoutUrl() {
        return String.format("%s/realms/%s/protocol/openid-connect/logout" +
                "?client_id=%s" +
                "&post_logout_redirect_uri=%s",
                keycloakUrl, realm, clientId,
                encodeURIComponent("http://localhost:3000"));
    }

    // Removed placeholder methods - now using KeycloakTokenService

    private String extractRefreshTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("kc_refresh_token".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    private String generateState() {
        return java.util.UUID.randomUUID().toString();
    }

    private String encodeURIComponent(String value) {
        try {
            return java.net.URLEncoder.encode(value, "UTF-8");
        } catch (Exception e) {
            return value;
        }
    }

    // DTO classes moved to KeycloakTokenService
}
