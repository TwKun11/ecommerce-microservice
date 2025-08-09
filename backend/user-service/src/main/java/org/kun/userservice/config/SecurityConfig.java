package org.kun.userservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.beans.factory.annotation.Value;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private String issuerUri;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        // Original custom endpoints
                        .requestMatchers("/api/auth/register", "/api/auth/login", "/api/auth/forgot-password", 
                                       "/api/auth/google-login", "/api/auth/reset-password-request", 
                                       "/api/auth/reset-password").permitAll()
                        // Simple auth endpoints (using Keycloak features)
                        .requestMatchers("/api/simple-auth/login", "/api/simple-auth/register", 
                                       "/api/simple-auth/forgot-password", "/api/simple-auth/health").permitAll()
                        .requestMatchers("/api/simple-auth/account", "/api/simple-auth/user", 
                                       "/api/simple-auth/logout").authenticated()
                        .requestMatchers("/api/simple-auth/admin/**").hasRole("ADMIN")
                        // OAuth2 login callback
                        .requestMatchers("/login/oauth2/code/**").permitAll()
                        .requestMatchers("/actuator/**").permitAll()
                        // Legacy endpoints
                        .requestMatchers("/api/auth/profile", "/api/auth/change-password").authenticated()
                        .requestMatchers("/api/auth/users").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .jwtAuthenticationConverter(jwtAuthenticationConverter())
                        )
                );
        return http.build();
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withIssuerLocation(issuerUri).build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwtGrantedAuthoritiesConverter());
        return converter;
    }

    @Bean
    public Converter<Jwt, Collection<GrantedAuthority>> jwtGrantedAuthoritiesConverter() {
        return jwt -> {
            Collection<GrantedAuthority> authorities = new ArrayList<>();
            
            // Extract realm roles
            Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
            if (realmAccess != null) {
                Collection<String> realmRoles = (Collection<String>) realmAccess.get("roles");
                if (realmRoles != null) {
                    realmRoles.stream()
                        .map(role -> "ROLE_" + role)
                        .map(SimpleGrantedAuthority::new)
                        .forEach(authorities::add);
                }
            }

            // Extract resource roles from all clients
            Map<String, Object> resourceAccess = jwt.getClaimAsMap("resource_access");
            if (resourceAccess != null) {
                for (Map.Entry<String, Object> entry : resourceAccess.entrySet()) {
                    String clientId = entry.getKey();
                    Map<String, Object> clientAccess = (Map<String, Object>) entry.getValue();
                    if (clientAccess != null && clientAccess.containsKey("roles")) {
                        Collection<String> clientRoles = (Collection<String>) clientAccess.get("roles");
                        if (clientRoles != null) {
                            clientRoles.stream()
                                .map(role -> "ROLE_" + role)
                                .map(SimpleGrantedAuthority::new)
                                .forEach(authorities::add);
                        }
                    }
                }
            }

            return authorities;
        };
    }
}