package org.kun.userservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/simple-auth")
@RequiredArgsConstructor
public class SimpleAuthController {

    @Value("${keycloak.auth-server-url}")
    private String keycloakUrl;

    @Value("${keycloak.realm}")
    private String realm;

    /**
     * Redirect to Keycloak login page
     */
    @GetMapping("/login")
    public ResponseEntity<?> login() {
        Map<String, String> response = new HashMap<>();
        response.put("loginUrl", "/oauth2/authorization/keycloak");
        response.put("keycloakDirectUrl", keycloakUrl + "/realms/" + realm + "/protocol/openid-connect/auth?client_id=user-service&response_type=code&redirect_uri=http://localhost:8083/login/oauth2/code/keycloak&scope=openid%20profile%20email");
        return ResponseEntity.ok(response);
    }

    /**
     * Redirect to Keycloak registration page
     */
    @GetMapping("/register")
    public ResponseEntity<?> register() {
        Map<String, String> response = new HashMap<>();
        response.put("registerUrl", keycloakUrl + "/realms/" + realm + "/protocol/openid-connect/registrations?client_id=user-service&response_type=code&redirect_uri=http://localhost:8083/login/oauth2/code/keycloak&scope=openid%20profile%20email");
        return ResponseEntity.ok(response);
    }

    /**
     * Redirect to Keycloak forgot password page
     */
    @GetMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword() {
        Map<String, String> response = new HashMap<>();
        response.put("forgotPasswordUrl", keycloakUrl + "/realms/" + realm + "/login-actions/reset-credentials?client_id=user-service&tab_id=1");
        return ResponseEntity.ok(response);
    }

    /**
     * Redirect to Keycloak account management
     */
    @GetMapping("/account")
    public ResponseEntity<?> account() {
        Map<String, String> response = new HashMap<>();
        response.put("accountUrl", keycloakUrl + "/realms/" + realm + "/account/");
        response.put("changePasswordUrl", keycloakUrl + "/realms/" + realm + "/account/password");
        response.put("profileUrl", keycloakUrl + "/realms/" + realm + "/account/personal-info");
        return ResponseEntity.ok(response);
    }

    /**
     * Get current user information from JWT
     */
    @GetMapping("/user")
    public ResponseEntity<?> getCurrentUser(@AuthenticationPrincipal Jwt jwt) {
        try {
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("id", jwt.getSubject());
            userInfo.put("username", jwt.getClaimAsString("preferred_username"));
            userInfo.put("email", jwt.getClaimAsString("email"));
            userInfo.put("firstName", jwt.getClaimAsString("given_name"));
            userInfo.put("lastName", jwt.getClaimAsString("family_name"));
            userInfo.put("emailVerified", jwt.getClaimAsBoolean("email_verified"));
            userInfo.put("roles", jwt.getClaimAsStringList("roles"));
            userInfo.put("realmAccess", jwt.getClaim("realm_access"));
            userInfo.put("resourceAccess", jwt.getClaim("resource_access"));

            return ResponseEntity.ok(userInfo);
        } catch (Exception e) {
            log.error("Error getting user info: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to get user information");
        }
    }

    /**
     * Logout endpoint - returns Keycloak logout URL
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        Map<String, String> response = new HashMap<>();
        response.put("logoutUrl", keycloakUrl + "/realms/" + realm + "/protocol/openid-connect/logout?redirect_uri=http://localhost:8083/");
        return ResponseEntity.ok(response);
    }

    /**
     * Admin endpoint - requires ADMIN role
     */
    @GetMapping("/admin/users")
    public ResponseEntity<?> getAdminInfo(@AuthenticationPrincipal Jwt jwt) {
        try {
            // Check if user has ADMIN role
            Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
            if (realmAccess != null && realmAccess.containsKey("roles")) {
                java.util.List<String> roles = (java.util.List<String>) realmAccess.get("roles");
                if (roles != null && roles.contains("ADMIN")) {
                    Map<String, String> response = new HashMap<>();
                    response.put("adminConsoleUrl", keycloakUrl + "/admin/" + realm + "/console/#/" + realm + "/users");
                    response.put("usersApiUrl", keycloakUrl + "/admin/realms/" + realm + "/users");
                    response.put("message", "Use Keycloak Admin Console or Admin API to manage users");
                    return ResponseEntity.ok(response);
                }
            }
            
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Access denied. Admin role required.");
        } catch (Exception e) {
            log.error("Error in admin endpoint: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to process admin request");
        }
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<?> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("keycloakUrl", keycloakUrl);
        health.put("realm", realm);
        health.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(health);
    }
}
