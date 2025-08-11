import React, {
  createContext,
  useContext,
  useState,
  useEffect,
  useCallback,
  ReactNode,
} from "react";
import { secureTokenManager } from "../../../shared/utils/secureTokenManager";
import {
  handleAuthCallback,
  login,
  logout,
  checkAuthStatus,
  startProactiveRefresh,
} from "../../../shared/utils/secureApiClient";

// Context types
interface SecureAuthContextType {
  // State
  isAuthenticated: boolean;
  loading: boolean;

  // Actions
  login: (redirectUri?: string) => void;
  logout: () => Promise<void>;

  // Utilities
  checkAuthStatus: () => boolean;
  getTimeUntilExpiry: () => number;
}

// Create context
const SecureAuthContext = createContext<SecureAuthContextType | undefined>(
  undefined
);

// Provider props
interface SecureAuthProviderProps {
  children: ReactNode;
}

/**
 * 🚀 SECURE AUTH CONTEXT - BIG TECH STANDARD
 *
 * Authorization Code Flow với Backend-only Token Handling:
 * ✅ Frontend không bao giờ touch refresh token
 * ✅ Memory-only access token
 * ✅ Automatic token refresh
 * ✅ Proper error handling
 */
export const SecureAuthProvider: React.FC<SecureAuthProviderProps> = ({
  children,
}) => {
  const [isAuthenticated, setIsAuthenticated] = useState<boolean>(false);
  const [loading, setLoading] = useState<boolean>(true);

  /**
   * Initialize authentication state
   */
  const initializeAuth = useCallback(async () => {
    try {
      setLoading(true);

      // 1. Check for auth callback in URL fragment
      const authSuccess = handleAuthCallback();

      if (authSuccess) {
        setIsAuthenticated(true);
        console.log("✅ Authentication initialized from callback");
      } else {
        // 2. Check if we have a valid token in memory
        const hasValidToken = checkAuthStatus();
        setIsAuthenticated(hasValidToken);

        if (hasValidToken) {
          console.log("✅ Authentication initialized from memory");
        } else {
          console.log("ℹ️ No valid authentication found");
        }
      }
    } catch (error) {
      console.error("❌ Auth initialization failed:", error);
      setIsAuthenticated(false);
    } finally {
      setLoading(false);
    }
  }, []);

  /**
   * Login function
   */
  const handleLogin = useCallback((redirectUri?: string) => {
    console.log("🔐 Initiating login...");
    login(redirectUri);
  }, []);

  /**
   * Logout function
   */
  const handleLogout = useCallback(async () => {
    try {
      console.log("🚪 Logging out...");
      await logout();
      setIsAuthenticated(false);
      console.log("✅ Logout completed");
    } catch (error) {
      console.error("❌ Logout failed:", error);
      // Force logout anyway
      setIsAuthenticated(false);
    }
  }, []);

  /**
   * Check authentication status
   */
  const handleCheckAuthStatus = useCallback(() => {
    return checkAuthStatus();
  }, []);

  /**
   * Get time until token expires
   */
  const getTimeUntilExpiry = useCallback(() => {
    return secureTokenManager.getTimeUntilExpiry();
  }, []);

  // Initialize auth on mount
  useEffect(() => {
    initializeAuth();
  }, [initializeAuth]);

  // Start proactive refresh timer
  useEffect(() => {
    if (isAuthenticated) {
      console.log("🔄 Starting proactive refresh timer");
      startProactiveRefresh();
    }
  }, [isAuthenticated]);

  // Listen for storage events (for multi-tab sync)
  useEffect(() => {
    const handleStorageChange = () => {
      const currentAuthStatus = checkAuthStatus();
      if (currentAuthStatus !== isAuthenticated) {
        setIsAuthenticated(currentAuthStatus);
      }
    };

    window.addEventListener("storage", handleStorageChange);
    return () => window.removeEventListener("storage", handleStorageChange);
  }, [isAuthenticated]);

  const contextValue: SecureAuthContextType = {
    isAuthenticated,
    loading,
    login: handleLogin,
    logout: handleLogout,
    checkAuthStatus: handleCheckAuthStatus,
    getTimeUntilExpiry,
  };

  return (
    <SecureAuthContext.Provider value={contextValue}>
      {children}
    </SecureAuthContext.Provider>
  );
};

/**
 * Hook to use secure authentication context
 */
export const useSecureAuth = (): SecureAuthContextType => {
  const context = useContext(SecureAuthContext);
  if (context === undefined) {
    throw new Error("useSecureAuth must be used within a SecureAuthProvider");
  }
  return context;
};

export default SecureAuthProvider;
