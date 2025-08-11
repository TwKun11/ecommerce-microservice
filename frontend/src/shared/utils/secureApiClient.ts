import axios, {
  type AxiosInstance,
  type AxiosRequestConfig,
  type AxiosResponse,
} from "axios";
import { secureTokenManager } from "./secureTokenManager";

/**
 * 🚀 SECURE API CLIENT - BIG TECH STANDARD
 *
 * Axios client với automatic token handling:
 * ✅ Automatic token injection
 * ✅ Automatic token refresh
 * ✅ Request queuing during refresh
 * ✅ Proper error handling
 */

// Create axios instance
const apiClient: AxiosInstance = axios.create({
  baseURL: "http://localhost:8083",
  timeout: 10000,
  withCredentials: true, // Include cookies
});

// Refresh state management
let isRefreshing = false;
let failedQueue: Array<{ resolve: Function; reject: Function }> = [];

const processQueue = (error: any, token: string | null = null) => {
  failedQueue.forEach((prom) => {
    if (error) {
      prom.reject(error);
    } else {
      prom.resolve(token);
    }
  });
  failedQueue = [];
};

// Request interceptor - inject token
apiClient.interceptors.request.use(
  (config: AxiosRequestConfig) => {
    const token = secureTokenManager.getAccessToken();
    if (token) {
      config.headers = config.headers || {};
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Response interceptor - handle 401 and refresh
apiClient.interceptors.response.use(
  (response: AxiosResponse) => {
    return response;
  },
  async (error) => {
    const originalRequest = error.config;

    if (error.response?.status === 401 && !originalRequest._retry) {
      if (isRefreshing) {
        // Queue request while refresh in progress
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject });
        })
          .then((token) => {
            originalRequest.headers.Authorization = `Bearer ${token}`;
            return apiClient(originalRequest);
          })
          .catch((err) => {
            return Promise.reject(err);
          });
      }

      originalRequest._retry = true;
      isRefreshing = true;

      try {
        const newToken = await secureTokenManager.refreshToken();

        if (newToken) {
          processQueue(null, newToken);

          originalRequest.headers.Authorization = `Bearer ${newToken}`;
          return apiClient(originalRequest);
        } else {
          throw new Error("Refresh failed");
        }
      } catch (refreshError) {
        processQueue(refreshError);
        secureTokenManager.clearToken();

        // Redirect to login
        window.location.href = `/api/auth/login?redirect_uri=${encodeURIComponent(
          window.location.href
        )}`;

        return Promise.reject(refreshError);
      } finally {
        isRefreshing = false;
      }
    }

    return Promise.reject(error);
  }
);

/**
 * Proactive token refresh timer
 */
export const startProactiveRefresh = () => {
  setInterval(async () => {
    if (secureTokenManager.isTokenNearExpiry(120)) {
      // 2 minutes before expiry
      try {
        await secureTokenManager.refreshToken();
        console.log("🔄 Token refreshed proactively");
      } catch (error) {
        console.warn("Proactive refresh failed:", error);
        // Reactive refresh will handle when API call fails
      }
    }
  }, 60000); // Check every minute
};

/**
 * Handle authentication callback from URL fragment
 */
export const handleAuthCallback = (): boolean => {
  const hash = window.location.hash;
  if (hash.includes("access_token")) {
    const params = new URLSearchParams(hash.substring(1));
    const accessToken = params.get("access_token");
    const expiresIn = parseInt(params.get("expires_in") || "0");
    const error = params.get("error");

    if (error) {
      console.error("Authentication failed:", params.get("error_description"));
      return false;
    }

    if (accessToken && expiresIn > 0) {
      // Store token in memory
      secureTokenManager.setAccessToken(accessToken, expiresIn);

      // Clean URL và redirect về home
      window.history.replaceState({}, document.title, "/");

      // Force redirect to home page
      setTimeout(() => {
        window.location.href = "http://localhost:5173/";
      }, 100);

      console.log("✅ Authentication successful - Redirecting to home");
      return true;
    }
  }
  return false;
};

/**
 * Login function
 */
export const login = (redirectUri?: string) => {
  const targetUri = redirectUri || window.location.href;
  const loginUrl = `http://localhost:8083/api/auth/login?redirect_uri=${encodeURIComponent(
    targetUri
  )}`;
  console.log("🔐 Redirecting to login:", loginUrl);
  window.location.href = loginUrl;
};

/**
 * Logout function
 */
export const logout = async () => {
  try {
    // Clear HttpOnly cookie
    await apiClient.post("/api/auth/logout");
  } catch (error) {
    console.warn("Logout API failed:", error);
  } finally {
    // Clear memory token
    secureTokenManager.clearToken();

    // Clear any session storage
    sessionStorage.clear();

    // Redirect to Keycloak logout
    window.location.href = "/api/auth/logout-redirect";
  }
};

/**
 * Check authentication status
 */
export const checkAuthStatus = () => {
  return secureTokenManager.isAuthenticated();
};

// Export the configured client
export default apiClient;

// Export convenience methods
export const api = {
  get: <T = any>(url: string, config?: AxiosRequestConfig) =>
    apiClient.get<T>(url, config),

  post: <T = any>(url: string, data?: any, config?: AxiosRequestConfig) =>
    apiClient.post<T>(url, data, config),

  put: <T = any>(url: string, data?: any, config?: AxiosRequestConfig) =>
    apiClient.put<T>(url, data, config),

  delete: <T = any>(url: string, config?: AxiosRequestConfig) =>
    apiClient.delete<T>(url, config),

  patch: <T = any>(url: string, data?: any, config?: AxiosRequestConfig) =>
    apiClient.patch<T>(url, data, config),
};
