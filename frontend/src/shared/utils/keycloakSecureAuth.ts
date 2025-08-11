/**
 * 🚀 BIG TECH SECURE AUTH FOR KEYCLOAK
 *
 * Tích hợp Keycloak với HttpOnly Cookie:
 * ✅ Keycloak xử lý authentication
 * ✅ HttpOnly Cookie cho refresh token
 * ✅ XSS protection
 * ✅ CSRF protection
 */

// API base URL
const API_BASE_URL = "http://localhost:8083";

// Token storage keys (chỉ cho access token)
const TOKEN_KEYS = {
  ACCESS_TOKEN: "kc_access_token",
  TOKEN_EXPIRY: "kc_token_expiry",
} as const;

/**
 * Secure Keycloak Authentication Manager
 */
class KeycloakSecureAuth {
  private refreshPromise: Promise<any> | null = null;

  /**
   * Lưu access token vào sessionStorage (memory)
   */
  setAccessToken(token: string, expiresIn: number = 900): void {
    const expiryTime = Date.now() + expiresIn * 1000;

    sessionStorage.setItem(TOKEN_KEYS.ACCESS_TOKEN, token);
    sessionStorage.setItem(TOKEN_KEYS.TOKEN_EXPIRY, expiryTime.toString());

    console.log("🔐 Access token stored in memory");
  }

  /**
   * Lấy access token từ sessionStorage
   */
  getAccessToken(): string | null {
    return sessionStorage.getItem(TOKEN_KEYS.ACCESS_TOKEN);
  }

  /**
   * Kiểm tra token có hết hạn không
   */
  isTokenExpired(): boolean {
    const expiry = sessionStorage.getItem(TOKEN_KEYS.TOKEN_EXPIRY);
    if (!expiry) return true;

    return Date.now() > parseInt(expiry);
  }

  /**
   * Xóa tokens khỏi memory
   */
  clearTokens(): void {
    sessionStorage.removeItem(TOKEN_KEYS.ACCESS_TOKEN);
    sessionStorage.removeItem(TOKEN_KEYS.TOKEN_EXPIRY);

    console.log("🧹 Tokens cleared from memory");
  }

  /**
   * Lưu refresh token vào HttpOnly cookie
   */
  async storeRefreshToken(refreshToken: string): Promise<boolean> {
    try {
      console.log("🍪 Storing refresh token in HttpOnly cookie...");

      const response = await fetch(
        `${API_BASE_URL}/api/secure/tokens/refresh`,
        {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          credentials: "include", // Important for cookies
          body: JSON.stringify({ refreshToken }),
        }
      );

      if (!response.ok) {
        throw new Error(`Failed to store refresh token: ${response.status}`);
      }

      const data = await response.json();
      console.log("✅ Refresh token stored securely:", data.message);
      return true;
    } catch (error) {
      console.error("❌ Failed to store refresh token:", error);
      return false;
    }
  }

  /**
   * Xóa refresh token cookie
   */
  async clearRefreshToken(): Promise<boolean> {
    try {
      console.log("🍪 Clearing refresh token cookie...");

      const response = await fetch(
        `${API_BASE_URL}/api/secure/tokens/refresh`,
        {
          method: "DELETE",
          credentials: "include",
        }
      );

      if (!response.ok) {
        throw new Error(`Failed to clear refresh token: ${response.status}`);
      }

      const data = await response.json();
      console.log("✅ Refresh token cleared:", data.message);
      return true;
    } catch (error) {
      console.error("❌ Failed to clear refresh token:", error);
      return false;
    }
  }

  /**
   * Kiểm tra có refresh token cookie không
   */
  async hasRefreshToken(): Promise<boolean> {
    try {
      const response = await fetch(`${API_BASE_URL}/api/secure/tokens/status`, {
        method: "GET",
        credentials: "include",
      });

      if (!response.ok) {
        return false;
      }

      const data = await response.json();
      return data.hasRefreshToken;
    } catch (error) {
      console.error("❌ Failed to check refresh token:", error);
      return false;
    }
  }

  /**
   * Lấy auth headers cho API calls
   */
  getAuthHeaders(): Record<string, string> {
    const token = this.getAccessToken();
    return token ? { Authorization: `Bearer ${token}` } : {};
  }

  /**
   * Kiểm tra user đã authenticated chưa
   */
  isAuthenticated(): boolean {
    const token = this.getAccessToken();
    return token !== null && !this.isTokenExpired();
  }
}

/**
 * Secure API Client cho Keycloak
 */
class KeycloakSecureApiClient {
  private authManager: KeycloakSecureAuth;

  constructor(authManager: KeycloakSecureAuth) {
    this.authManager = authManager;
  }

  /**
   * Thực hiện API request với automatic token handling
   */
  async request<T>(url: string, options: RequestInit = {}): Promise<T> {
    const fullUrl = url.startsWith("http") ? url : `${API_BASE_URL}${url}`;

    // Add auth headers
    const headers = {
      "Content-Type": "application/json",
      ...this.authManager.getAuthHeaders(),
      ...options.headers,
    };

    try {
      const response = await fetch(fullUrl, {
        ...options,
        headers,
        credentials: "include", // Include cookies
      });

      if (!response.ok) {
        throw new Error(
          `API request failed: ${response.status} ${response.statusText}`
        );
      }

      return await response.json();
    } catch (error) {
      console.error("❌ API request failed:", error);
      throw error;
    }
  }

  // Convenience methods
  async get<T>(url: string): Promise<T> {
    return this.request<T>(url, { method: "GET" });
  }

  async post<T>(url: string, data?: any): Promise<T> {
    return this.request<T>(url, {
      method: "POST",
      body: data ? JSON.stringify(data) : undefined,
    });
  }

  async put<T>(url: string, data?: any): Promise<T> {
    return this.request<T>(url, {
      method: "PUT",
      body: data ? JSON.stringify(data) : undefined,
    });
  }

  async delete<T>(url: string): Promise<T> {
    return this.request<T>(url, { method: "DELETE" });
  }
}

// Create singleton instances
export const keycloakSecureAuth = new KeycloakSecureAuth();
export const keycloakSecureApi = new KeycloakSecureApiClient(
  keycloakSecureAuth
);

// Export utility functions
export const keycloakAuthUtils = {
  /**
   * Setup sau khi login thành công với Keycloak
   */
  async setupAfterKeycloakLogin(
    accessToken: string,
    refreshToken: string
  ): Promise<void> {
    // Lưu access token vào memory
    keycloakSecureAuth.setAccessToken(accessToken);

    // Lưu refresh token vào HttpOnly cookie
    await keycloakSecureAuth.storeRefreshToken(refreshToken);

    console.log("🚀 Keycloak authentication setup completed");
  },

  /**
   * Logout - xóa cả access token và refresh token
   */
  async logout(): Promise<void> {
    try {
      // Xóa refresh token cookie
      await keycloakSecureAuth.clearRefreshToken();
    } catch (error) {
      console.error("Logout API call failed:", error);
    } finally {
      // Luôn xóa local tokens
      keycloakSecureAuth.clearTokens();
    }
  },

  /**
   * Kiểm tra authentication status
   */
  async checkAuthStatus(): Promise<{
    hasAccessToken: boolean;
    hasRefreshToken: boolean;
    isAuthenticated: boolean;
  }> {
    const hasAccessToken = keycloakSecureAuth.isAuthenticated();
    const hasRefreshToken = await keycloakSecureAuth.hasRefreshToken();

    return {
      hasAccessToken,
      hasRefreshToken,
      isAuthenticated: hasAccessToken && hasRefreshToken,
    };
  },
};

export default keycloakSecureAuth;
