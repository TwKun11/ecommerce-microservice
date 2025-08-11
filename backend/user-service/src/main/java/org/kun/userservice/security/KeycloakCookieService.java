package org.kun.userservice.security;

import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;


@Service
public class KeycloakCookieService {

    @Value("${app.security.refresh-token.expiration:2592000}") // 30 days default
    private long refreshTokenExpiration;

    @Value("${app.security.cookie.domain:localhost}")
    private String cookieDomain;

    @Value("${app.security.cookie.secure:false}")
    private boolean cookieSecure;

    /**
     * Tạo HttpOnly cookie cho refresh token
     */
    public ResponseCookie createRefreshTokenCookie(String refreshToken) {
        return ResponseCookie.from("kc_refresh_token", refreshToken)
                .httpOnly(true)
                .secure(cookieSecure) // true for HTTPS, false for development
                .sameSite("Strict")
                .path("/api/auth/refresh")
                .maxAge(Duration.ofSeconds(refreshTokenExpiration))
                .domain(cookieDomain)
                .build();
    }

    /**
     * Xóa refresh token cookie (logout)
     */
    public ResponseCookie clearRefreshTokenCookie() {
        return ResponseCookie.from("kc_refresh_token", "")
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Strict")
                .path("/api/auth/refresh")
                .maxAge(Duration.ZERO)
                .domain(cookieDomain)
                .build();
    }

    /**
     * Lấy refresh token từ HttpOnly cookie
     */
    public String extractRefreshTokenFromCookie(HttpServletRequest request) {
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

    /**
     * Kiểm tra xem có refresh token cookie không
     */
    public boolean hasRefreshTokenCookie(HttpServletRequest request) {
        return extractRefreshTokenFromCookie(request) != null;
    }
}
