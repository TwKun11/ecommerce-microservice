package org.kun.userservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.representations.idm.UserRepresentation;
import org.kun.userservice.dto.*;
import org.kun.userservice.service.UserService;
import org.kun.userservice.service.GoogleAuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final GoogleAuthService googleAuthService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse> register(@RequestBody RegistrationRequest request) {
        try {
            ApiResponse response = userService.registerUser(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Registration error: ", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(false, "Registration failed: " + e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            LoginResponse response = userService.loginUser(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Login error: ", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse(false, "Login failed: " + e.getMessage()));
        }
    }

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(@AuthenticationPrincipal Jwt jwt) {
        try {
            String userId = jwt.getSubject();
            UserRepresentation user = userService.getUserProfile(userId);
            
            Map<String, Object> profile = new HashMap<>();
            profile.put("id", user.getId());
            profile.put("username", user.getUsername());
            profile.put("email", user.getEmail());
            profile.put("firstName", user.getFirstName());
            profile.put("lastName", user.getLastName());
            profile.put("emailVerified", user.isEmailVerified());
            profile.put("enabled", user.isEnabled());
            
            return ResponseEntity.ok(new ApiResponse(true, "Profile retrieved successfully", profile));
        } catch (Exception e) {
            log.error("Error getting profile: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Failed to get profile: " + e.getMessage()));
        }
    }

    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse> changePassword(
            @RequestBody ChangePasswordRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        try {
            String userId = jwt.getSubject();
            ApiResponse response = userService.changePassword(userId, request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Change password error: ", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(false, "Failed to change password: " + e.getMessage()));
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        try {
            ApiResponse response = userService.forgotPassword(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Forgot password error: ", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(false, "Failed to send reset email: " + e.getMessage()));
        }
    }

    @PostMapping("/google-login")
    public ResponseEntity<?> googleLogin(@RequestBody GoogleLoginRequest request) {
        try {
            LoginResponse response = googleAuthService.loginWithGoogle(request.getIdToken(), request.isRememberMe());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Google login error: ", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse(false, "Google login failed: " + e.getMessage()));
        }
    }

    @PostMapping("/reset-password-request")
    public ResponseEntity<ApiResponse> requestPasswordReset(@RequestBody ForgotPasswordRequest request) {
        try {
            ApiResponse response = userService.initiatePasswordReset(request.getEmail());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Password reset request error: ", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(false, "Failed to process password reset request: " + e.getMessage()));
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        try {
            ApiResponse response = userService.resetPasswordWithToken(request.getToken(), request.getNewPassword());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Password reset error: ", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(false, "Failed to reset password: " + e.getMessage()));
        }
    }

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAllUsers(@AuthenticationPrincipal Jwt jwt) {
        try {
            String userId = jwt.getSubject();
            
            // Double-check admin role
            if (!userService.hasAdminRole(userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ApiResponse(false, "Access denied. Admin role required."));
            }
            
            List<UserRepresentation> users = userService.getAllUsers();
            
            // Filter sensitive information
            List<Map<String, Object>> userList = users.stream().map(user -> {
                Map<String, Object> userMap = new HashMap<>();
                userMap.put("id", user.getId());
                userMap.put("username", user.getUsername());
                userMap.put("email", user.getEmail());
                userMap.put("firstName", user.getFirstName());
                userMap.put("lastName", user.getLastName());
                userMap.put("enabled", user.isEnabled());
                userMap.put("emailVerified", user.isEmailVerified());
                userMap.put("createdTimestamp", user.getCreatedTimestamp());
                return userMap;
            }).toList();
            
            return ResponseEntity.ok(new ApiResponse(true, "Users retrieved successfully", userList));
        } catch (Exception e) {
            log.error("Error getting all users: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Failed to get users: " + e.getMessage()));
        }
    }
}