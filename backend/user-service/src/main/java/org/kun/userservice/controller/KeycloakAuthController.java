package org.kun.userservice.controller;

import org.kun.userservice.security.KeycloakCookieService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
@RequestMapping("/api/secure")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class KeycloakAuthController {

    private final KeycloakCookieService cookieService;

    public KeycloakAuthController(KeycloakCookieService cookieService) {
        this.cookieService = cookieService;
    }

    @PostMapping("/tokens/refresh")
    public ResponseEntity<?> storeRefreshToken(
            @RequestBody StoreRefreshTokenRequest request,
            HttpServletRequest httpRequest) {
        
        try {
            // Tạo HttpOnly cookie cho refresh token
            var refreshTokenCookie = cookieService.createRefreshTokenCookie(request.refreshToken);
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString())
                    .body(Map.of(
                        "message", "Refresh token stored securely",
                        "success", true
                    ));
                    
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                        "error", "Failed to store refresh token",
                        "message", e.getMessage()
                    ));
        }
    }

    /**
     * Kiểm tra trạng thái refresh token
     */
    @GetMapping("/tokens/status")
    public ResponseEntity<?> getTokenStatus(HttpServletRequest request) {
        boolean hasToken = cookieService.hasRefreshTokenCookie(request);
        
        return ResponseEntity.ok(Map.of(
            "hasRefreshToken", hasToken,
            "message", hasToken ? "Refresh token found" : "No refresh token found"
        ));
    }

    /**
     * Xóa refresh token cookie (logout)
     */
    @DeleteMapping("/tokens/refresh")
    public ResponseEntity<?> clearRefreshToken() {
        try {
            var clearCookie = cookieService.clearRefreshTokenCookie();
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, clearCookie.toString())
                    .body(Map.of(
                        "message", "Refresh token cleared",
                        "success", true
                    ));
                    
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                        "error", "Failed to clear refresh token",
                        "message", e.getMessage()
                    ));
        }
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<?> health() {
        return ResponseEntity.ok(Map.of(
            "status", "healthy",
            "service", "SecureTokenController",
            "message", "Big Tech security ready"
        ));
    }

    // DTO for storing refresh token
    public static class StoreRefreshTokenRequest {
        private String refreshToken;

        public String getRefreshToken() { return refreshToken; }
        public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
    }
}
