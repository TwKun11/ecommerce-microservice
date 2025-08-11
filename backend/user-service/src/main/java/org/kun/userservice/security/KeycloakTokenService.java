package org.kun.userservice.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.stream.Collectors;

/**
 * 🔧 KEYCLOAK TOKEN SERVICE
 * 
 * Service để handle token exchange với Keycloak:
 * ✅ Exchange authorization code for tokens
 * ✅ Refresh tokens using refresh token
 * ✅ Parse token responses
 */
@Service
public class KeycloakTokenService {
    
    @Value("${keycloak.auth-server-url}")
    private String keycloakUrl;
    
    @Value("${keycloak.realm}")
    private String realm;
    
    @Value("${keycloak.resource}")
    private String clientId;
    
    @Value("${keycloak.credentials.secret}")
    private String clientSecret;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Exchange authorization code for tokens
     */
    public TokenResponse exchangeCodeForTokens(String code) throws Exception {
        String tokenUrl = String.format("%s/realms/%s/protocol/openid-connect/token", 
                                       keycloakUrl, realm);
        
        // Prepare form data
        String formData = String.format(
            "grant_type=authorization_code" +
            "&client_id=%s" +
            "&client_secret=%s" +
            "&code=%s" +
            "&redirect_uri=%s",
            clientId, clientSecret, code,
            encodeURIComponent("http://localhost:8083/api/auth/callback")
        );
        
        return makeTokenRequest(tokenUrl, formData);
    }
    
    /**
     * Refresh tokens using refresh token
     */
    public TokenResponse refreshTokens(String refreshToken) throws Exception {
        String tokenUrl = String.format("%s/realms/%s/protocol/openid-connect/token", 
                                       keycloakUrl, realm);
        
        // Prepare form data
        String formData = String.format(
            "grant_type=refresh_token" +
            "&client_id=%s" +
            "&client_secret=%s" +
            "&refresh_token=%s",
            clientId, clientSecret, refreshToken
        );
        
        return makeTokenRequest(tokenUrl, formData);
    }
    
    /**
     * Make HTTP request to Keycloak token endpoint
     */
    private TokenResponse makeTokenRequest(String tokenUrl, String formData) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(tokenUrl).openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        connection.setDoOutput(true);
        
        try (OutputStream os = connection.getOutputStream()) {
            os.write(formData.getBytes());
        }
        
        // Parse response
        if (connection.getResponseCode() == 200) {
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(connection.getInputStream()))) {
                String response = br.lines().collect(Collectors.joining());
                return parseTokenResponse(response);
            }
        } else {
            // Read error response
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(connection.getErrorStream()))) {
                String errorResponse = br.lines().collect(Collectors.joining());
                throw new Exception("Token request failed: " + connection.getResponseCode() + 
                                  " - " + errorResponse);
            }
        }
    }
    
    /**
     * Parse JSON token response
     */
    private TokenResponse parseTokenResponse(String jsonResponse) throws Exception {
        try {
            JsonNode node = objectMapper.readTree(jsonResponse);
            
            return new TokenResponse(
                node.get("access_token").asText(),
                node.get("refresh_token").asText(),
                node.get("expires_in").asInt(),
                node.get("token_type").asText()
            );
        } catch (Exception e) {
            throw new Exception("Failed to parse token response: " + e.getMessage());
        }
    }
    
    /**
     * URL encode helper
     */
    private String encodeURIComponent(String value) {
        try {
            return java.net.URLEncoder.encode(value, "UTF-8");
        } catch (Exception e) {
            return value;
        }
    }
    
    /**
     * Token Response DTO
     */
    public static class TokenResponse {
        private String accessToken;
        private String refreshToken;
        private int expiresIn;
        private String tokenType;
        
        public TokenResponse(String accessToken, String refreshToken, int expiresIn, String tokenType) {
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
            this.expiresIn = expiresIn;
            this.tokenType = tokenType;
        }
        
        // Getters
        public String getAccessToken() { return accessToken; }
        public String getRefreshToken() { return refreshToken; }
        public int getExpiresIn() { return expiresIn; }
        public String getTokenType() { return tokenType; }
    }
}
