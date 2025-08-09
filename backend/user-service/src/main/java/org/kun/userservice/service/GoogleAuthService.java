package org.kun.userservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.kun.userservice.dto.ApiResponse;
import org.kun.userservice.dto.LoginResponse;
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
@RequiredArgsConstructor
public class GoogleAuthService {

    private final Keycloak keycloak;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.auth-server-url}")
    private String authServerUrl;

    @Value("${keycloak.resource}")
    private String clientId;

    @Value("${keycloak.credentials.secret}")
    private String clientSecret;

    public LoginResponse loginWithGoogle(String idToken, boolean rememberMe) {
        try {
            JsonNode googleUserInfo = verifyGoogleToken(idToken);
            
            String email = googleUserInfo.get("email").asText();
            String name = googleUserInfo.get("name").asText();
            String givenName = googleUserInfo.has("given_name") ? googleUserInfo.get("given_name").asText() : "";
            String familyName = googleUserInfo.has("family_name") ? googleUserInfo.get("family_name").asText() : "";
            String googleId = googleUserInfo.get("sub").asText();

            // Check if user exists in Keycloak
            UsersResource usersResource = keycloak.realm(realm).users();
            List<UserRepresentation> existingUsers = usersResource.search(null, null, null, email, 0, 1);

            UserRepresentation user;
            if (existingUsers.isEmpty()) {
                // Create new user
                user = createGoogleUser(email, name, givenName, familyName, googleId);
            } else {
                user = existingUsers.get(0);
                // Update Google ID if not set
                updateUserGoogleId(user, googleId);
            }

            // Generate Keycloak token for the user
            return generateTokenForUser(user.getUsername(), rememberMe);
            
        } catch (Exception e) {
            log.error("Error during Google login: ", e);
            throw new RuntimeException("Google login failed: " + e.getMessage());
        }
    }

    private JsonNode verifyGoogleToken(String idToken) {
        try {
            String url = "https://oauth2.googleapis.com/tokeninfo?id_token=" + idToken;
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            
            if (response.getStatusCode() == HttpStatus.OK) {
                return objectMapper.readTree(response.getBody());
            } else {
                throw new RuntimeException("Invalid Google token");
            }
        } catch (Exception e) {
            log.error("Error verifying Google token: ", e);
            throw new RuntimeException("Failed to verify Google token");
        }
    }

    private UserRepresentation createGoogleUser(String email, String name, String givenName, String familyName, String googleId) {
        try {
            UserRepresentation user = new UserRepresentation();
            user.setUsername(email); // Use email as username for Google users
            user.setEmail(email);
            user.setFirstName(givenName);
            user.setLastName(familyName);
            user.setEnabled(true);
            user.setEmailVerified(true); // Google emails are already verified

            // Set Google ID as an attribute
            Map<String, List<String>> attributes = new HashMap<>();
            attributes.put("google_id", Collections.singletonList(googleId));
            attributes.put("auth_provider", Collections.singletonList("google"));
            user.setAttributes(attributes);

            // Create a random password (user won't use it for Google login)
            CredentialRepresentation credential = new CredentialRepresentation();
            credential.setType(CredentialRepresentation.PASSWORD);
            credential.setValue(UUID.randomUUID().toString());
            credential.setTemporary(false);
            user.setCredentials(Collections.singletonList(credential));

            UsersResource usersResource = keycloak.realm(realm).users();
            Response response = usersResource.create(user);

            if (response.getStatus() == 201) {
                String location = response.getLocation().getPath();
                String userId = location.substring(location.lastIndexOf('/') + 1);

                // Assign USER role
                try {
                    RoleRepresentation userRole = keycloak.realm(realm).roles().get("USER").toRepresentation();
                    usersResource.get(userId).roles().realmLevel().add(Collections.singletonList(userRole));
                } catch (Exception e) {
                    log.warn("Could not assign USER role to Google user {}: {}", userId, e.getMessage());
                }

                // Get the created user
                user = usersResource.get(userId).toRepresentation();
                response.close();
                return user;
            } else {
                String errorMsg = response.readEntity(String.class);
                response.close();
                throw new RuntimeException("Failed to create Google user: " + errorMsg);
            }
        } catch (Exception e) {
            log.error("Error creating Google user: ", e);
            throw new RuntimeException("Failed to create Google user");
        }
    }

    private void updateUserGoogleId(UserRepresentation user, String googleId) {
        try {
            Map<String, List<String>> attributes = user.getAttributes();
            if (attributes == null) {
                attributes = new HashMap<>();
            }
            
            if (!attributes.containsKey("google_id")) {
                attributes.put("google_id", Collections.singletonList(googleId));
                attributes.put("auth_provider", Collections.singletonList("google"));
                user.setAttributes(attributes);
                
                keycloak.realm(realm).users().get(user.getId()).update(user);
            }
        } catch (Exception e) {
            log.error("Error updating Google ID for user: ", e);
            // Don't fail the login for this
        }
    }

    private LoginResponse generateTokenForUser(String username, boolean rememberMe) {
        try {
            String tokenUrl = authServerUrl + "/realms/" + realm + "/protocol/openid-connect/token";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("grant_type", "password");
            body.add("client_id", clientId);
            body.add("client_secret", clientSecret);
            body.add("username", username);
            body.add("password", "google-auth-" + UUID.randomUUID()); // This won't work, need different approach
            body.add("scope", "openid profile email");

            // For Google users, we need to use a different approach
            // We'll use the admin client to generate a token
            return generateTokenUsingAdminClient(username, rememberMe);

        } catch (Exception e) {
            log.error("Error generating token for Google user: ", e);
            throw new RuntimeException("Failed to generate token for Google user");
        }
    }

    private LoginResponse generateTokenUsingAdminClient(String username, boolean rememberMe) {
        // For now, return a simple response indicating Google login success
        // In a real implementation, you might want to generate a custom JWT
        // or use Keycloak's token exchange functionality
        throw new RuntimeException("Google token generation not fully implemented. Please configure Keycloak identity provider for Google.");
    }
}
