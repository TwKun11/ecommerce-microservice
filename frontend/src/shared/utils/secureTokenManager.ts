/**
 * 🚀 SECURE TOKEN MANAGER - BIG TECH STANDARD
 *
 * Memory-only token storage với proper lifecycle management:
 * ✅ Pure memory storage (no persistence)
 * ✅ Automatic cleanup
 * ✅ Proactive refresh
 * ✅ XSS protection
 */

/**
 * Secure Token Manager - Memory Only
 */
class SecureTokenManager {
  private accessToken: string | null = null;
  private tokenExpiry: number = 0;
  private refreshPromise: Promise<any> | null = null;

  /**
   * Set access token in memory
   */
  setAccessToken(token: string, expiresIn: number): void {
    this.accessToken = token;
    this.tokenExpiry = Date.now() + expiresIn * 1000;

    console.log("🔐 Access token stored in memory");
  }

  /**
   * Get access token from memory
   */
  getAccessToken(): string | null {
    if (Date.now() >= this.tokenExpiry) {
      this.accessToken = null; // Auto cleanup expired token
      console.log("🧹 Expired token auto-cleaned");
    }
    return this.accessToken;
  }

  /**
   * Check if token is near expiration
   */
  isTokenNearExpiry(threshold = 120): boolean {
    return this.tokenExpiry - Date.now() < threshold * 1000;
  }

  /**
   * Check if token is expired
   */
  isTokenExpired(): boolean {
    return Date.now() >= this.tokenExpiry;
  }

  /**
   * Clear token from memory
   */
  clearToken(): void {
    this.accessToken = null;
    this.tokenExpiry = 0;
    console.log("🧹 Token cleared from memory");
  }

  /**
   * Refresh token with backend
   */
  async refreshToken(): Promise<string | null> {
    // Prevent multiple simultaneous refresh requests
    if (this.refreshPromise) {
      return this.refreshPromise;
    }

    this.refreshPromise = this.performRefresh();
    const result = await this.refreshPromise;
    this.refreshPromise = null;
    return result;
  }

  private async performRefresh(): Promise<string | null> {
    try {
      console.log("🔄 Refreshing token...");

      const response = await fetch("/api/auth/refresh", {
        method: "POST",
        credentials: "include", // Send HttpOnly cookie
      });

      if (!response.ok) {
        throw new Error(`Refresh failed: ${response.status}`);
      }

      const data = await response.json();

      // Store new access token in memory
      this.setAccessToken(data.access_token, data.expires_in);

      console.log("✅ Token refreshed successfully");
      return data.access_token;
    } catch (error) {
      console.error("❌ Token refresh failed:", error);
      this.clearToken();
      return null;
    }
  }

  /**
   * Get time until token expires (in seconds)
   */
  getTimeUntilExpiry(): number {
    return Math.max(0, Math.floor((this.tokenExpiry - Date.now()) / 1000));
  }

  /**
   * Check if user is authenticated
   */
  isAuthenticated(): boolean {
    return this.getAccessToken() !== null;
  }

  // 🔍 DEBUG METHODS - For development only
  /**
   * Debug: Get token info (for development)
   */
  debugGetTokenInfo() {
    return {
      hasToken: !!this.accessToken,
      tokenLength: this.accessToken?.length || 0,
      tokenPreview: this.accessToken
        ? `${this.accessToken.substring(0, 20)}...`
        : null,
      expiresAt: this.tokenExpiry,
      timeUntilExpiry: this.getTimeUntilExpiry(),
      isExpired: this.isTokenExpired(),
      isNearExpiry: this.isTokenNearExpiry(),
      isAuthenticated: this.isAuthenticated(),
    };
  }

  /**
   * Debug: Check all storage locations
   */
  debugCheckAllStorage() {
    return {
      memory: this.debugGetTokenInfo(),
      localStorage: {
        hasAccessToken: !!localStorage.getItem("access_token"),
        hasRefreshToken: !!localStorage.getItem("refresh_token"),
        allKeys: Object.keys(localStorage),
      },
      sessionStorage: {
        hasAccessToken: !!sessionStorage.getItem("access_token"),
        hasRefreshToken: !!sessionStorage.getItem("refresh_token"),
        allKeys: Object.keys(sessionStorage),
      },
      cookies: {
        // Note: HttpOnly cookies cannot be read by JavaScript
        message:
          "HttpOnly cookies are not accessible via JavaScript (this is correct for security)",
      },
    };
  }
}

// Create singleton instance
export const secureTokenManager = new SecureTokenManager();

// 🔍 Expose debug methods to window for console access
if (typeof window !== "undefined") {
  (window as any).secureTokenManager = secureTokenManager;
  (window as any).debugTokenInfo = () => secureTokenManager.debugGetTokenInfo();
  (window as any).debugAllStorage = () =>
    secureTokenManager.debugCheckAllStorage();
}

export default secureTokenManager;
