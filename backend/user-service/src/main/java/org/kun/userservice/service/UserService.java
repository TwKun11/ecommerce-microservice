package org.kun.userservice.service;

import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.kun.userservice.dto.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import jakarta.ws.rs.core.Response;
import java.util.*;

@Slf4j
@Service
public class UserService {

    private final Keycloak keycloak;
    private final RestTemplate restTemplate;
    private final EmailService emailService;

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.auth-server-url}")
    private String authServerUrl;

    @Value("${keycloak.resource}")
    private String clientId;

    @Value("${keycloak.credentials.secret}")
    private String clientSecret;

    // Store for password reset tokens (in production, use Redis or database)
    private final Map<String, PasswordResetToken> resetTokens = new HashMap<>();

    public UserService(Keycloak keycloak, EmailService emailService) {
        this.keycloak = keycloak;
        this.emailService = emailService;
        this.restTemplate = new RestTemplate();
    }

    // Inner class for password reset token
    private static class PasswordResetToken {
        private final String userId;
        private final long expiryTime;

        public PasswordResetToken(String userId) {
            this.userId = userId;
            this.expiryTime = System.currentTimeMillis() + (60 * 60 * 1000); // 1 hour
        }

        public String getUserId() { return userId; }
        public boolean isExpired() { return System.currentTimeMillis() > expiryTime; }
    }

    public ApiResponse registerUser(RegistrationRequest request) {
        try {
            // Tạo user representation
            UserRepresentation user = new UserRepresentation();
            user.setUsername(request.getUsername());
            user.setEmail(request.getEmail());
            user.setFirstName(request.getFirstName());
            user.setLastName(request.getLastName());
            user.setEnabled(true);
            user.setEmailVerified(false);

            // Tạo credential
            CredentialRepresentation credential = new CredentialRepresentation();
            credential.setType(CredentialRepresentation.PASSWORD);
            credential.setValue(request.getPassword());
            credential.setTemporary(false);

            user.setCredentials(Collections.singletonList(credential));

            UsersResource usersResource = keycloak.realm(realm).users();
            Response response = usersResource.create(user);

            if (response.getStatus() == 201) {
                String location = response.getLocation().getPath();
                String userId = location.substring(location.lastIndexOf('/') + 1);

                // Gán role USER (nếu tồn tại)
                try {
                    RoleRepresentation userRole = keycloak.realm(realm)
                            .roles().get("USER").toRepresentation();
                    UserResource userResource = usersResource.get(userId);
                    userResource.roles().realmLevel().add(Collections.singletonList(userRole));
                    log.info("Successfully assigned USER role to user: {}", userId);
                } catch (Exception e) {
                    log.warn("Could not assign USER role to user {}: {}", userId, e.getMessage());
                    // Continue without failing the registration
                }

                response.close();
                return new ApiResponse(true, "User registered successfully");
            } else {
                String errorMsg = response.readEntity(String.class);
                response.close();
                return new ApiResponse(false, "Registration failed: " + errorMsg);
            }
        } catch (Exception e) {
            log.error("Error registering user: ", e);
            return new ApiResponse(false, "Registration failed: " + e.getMessage());
        }
    }
    public LoginResponse loginUser(LoginRequest request) {
        try {
            String tokenUrl = authServerUrl + "/realms/" + realm + "/protocol/openid-connect/token";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("grant_type", "password");
            body.add("client_id", clientId);
            body.add("client_secret", clientSecret);
            body.add("username", request.getUsername());
            body.add("password", request.getPassword());
            body.add("scope", "openid profile email");
            
            // Add remember me parameter if needed
            if (request.isRememberMe()) {
                // This would typically extend token lifetime
                // For now, we'll handle it in the response
            }

            HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    tokenUrl,
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                Map<String, Object> tokenResponse = response.getBody();
                return new LoginResponse(
                        (String) tokenResponse.get("access_token"),
                        (String) tokenResponse.get("refresh_token"),
                        ((Integer) tokenResponse.get("expires_in")).longValue(),
                        "Bearer"
                );
            } else {
                throw new RuntimeException("Login failed");
            }
        } catch (Exception e) {
            log.error("Error during login: ", e);
            throw new RuntimeException("Login failed: " + e.getMessage());
        }
    }

    public UserRepresentation getUserProfile(String userId) {
        try {
            return keycloak.realm(realm).users().get(userId).toRepresentation();
        } catch (Exception e) {
            log.error("Error getting user profile: ", e);
            throw new RuntimeException("Failed to get user profile: " + e.getMessage());
        }
    }

    public ApiResponse changePassword(String userId, ChangePasswordRequest request) {
        try {
            UserResource userResource = keycloak.realm(realm).users().get(userId);
            
            // Create new password credential
            CredentialRepresentation credential = new CredentialRepresentation();
            credential.setType(CredentialRepresentation.PASSWORD);
            credential.setValue(request.getNewPassword());
            credential.setTemporary(false);

            userResource.resetPassword(credential);
            
            return new ApiResponse(true, "Password changed successfully");
        } catch (Exception e) {
            log.error("Error changing password: ", e);
            return new ApiResponse(false, "Failed to change password: " + e.getMessage());
        }
    }

    public ApiResponse forgotPassword(ForgotPasswordRequest request) {
        try {
            // Find user by email
            List<UserRepresentation> users = keycloak.realm(realm).users()
                    .search(null, null, null, request.getEmail(), 0, 1);

            if (users.isEmpty()) {
                return new ApiResponse(false, "Email not found");
            }

            UserRepresentation user = users.get(0);
            
            // Execute forgot password actions
            keycloak.realm(realm).users().get(user.getId())
                    .executeActionsEmail(Collections.singletonList("UPDATE_PASSWORD"));
            
            return new ApiResponse(true, "Password reset email sent successfully");
        } catch (Exception e) {
            log.error("Error sending forgot password email: ", e);
            return new ApiResponse(false, "Failed to send reset email: " + e.getMessage());
        }
    }

    public ApiResponse initiatePasswordReset(String email) {
        try {
            // Find user by email
            List<UserRepresentation> users = keycloak.realm(realm).users()
                    .search(null, null, null, email, 0, 1);

            if (users.isEmpty()) {
                return new ApiResponse(false, "Email not found");
            }

            UserRepresentation user = users.get(0);
            
            // Generate reset token
            String resetToken = UUID.randomUUID().toString();
            resetTokens.put(resetToken, new PasswordResetToken(user.getId()));
            
            // Send email
            emailService.sendPasswordResetEmail(email, resetToken);
            
            return new ApiResponse(true, "Password reset email sent successfully");
        } catch (Exception e) {
            log.error("Error initiating password reset: ", e);
            return new ApiResponse(false, "Failed to initiate password reset: " + e.getMessage());
        }
    }

    public ApiResponse resetPasswordWithToken(String token, String newPassword) {
        try {
            PasswordResetToken resetToken = resetTokens.get(token);
            if (resetToken == null) {
                return new ApiResponse(false, "Invalid reset token");
            }
            
            if (resetToken.isExpired()) {
                resetTokens.remove(token);
                return new ApiResponse(false, "Reset token has expired");
            }
            
            // Reset password
            UserResource userResource = keycloak.realm(realm).users().get(resetToken.getUserId());
            CredentialRepresentation credential = new CredentialRepresentation();
            credential.setType(CredentialRepresentation.PASSWORD);
            credential.setValue(newPassword);
            credential.setTemporary(false);

            userResource.resetPassword(credential);
            
            // Remove used token
            resetTokens.remove(token);
            
            return new ApiResponse(true, "Password reset successfully");
        } catch (Exception e) {
            log.error("Error resetting password with token: ", e);
            return new ApiResponse(false, "Failed to reset password: " + e.getMessage());
        }
    }

    public List<UserRepresentation> getAllUsers() {
        try {
            return keycloak.realm(realm).users().list();
        } catch (Exception e) {
            log.error("Error getting all users: ", e);
            throw new RuntimeException("Failed to get all users: " + e.getMessage());
        }
    }

    public boolean hasAdminRole(String userId) {
        try {
            UserResource userResource = keycloak.realm(realm).users().get(userId);
            return userResource.roles().realmLevel().listAll().stream()
                    .anyMatch(role -> "ADMIN".equals(role.getName()));
        } catch (Exception e) {
            log.error("Error checking admin role for user: {}", userId, e);
            return false;
        }
    }
}
