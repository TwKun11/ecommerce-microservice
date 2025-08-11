import { useSecureAuth } from "../context/SecureAuthContext";

// Re-export useSecureAuth as useAuth for backward compatibility
export const useAuth = useSecureAuth;
