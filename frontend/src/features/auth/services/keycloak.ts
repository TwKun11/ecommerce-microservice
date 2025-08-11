import Keycloak from "keycloak-js";

// Validate environment variables
const requiredEnvVars = {
  VITE_KEYCLOAK_URL: import.meta.env.VITE_KEYCLOAK_URL,
  VITE_KEYCLOAK_REALM: import.meta.env.VITE_KEYCLOAK_REALM,
  VITE_KEYCLOAK_CLIENT_ID: import.meta.env.VITE_KEYCLOAK_CLIENT_ID,
};

// Check if all required environment variables are present
Object.entries(requiredEnvVars).forEach(([key, value]) => {
  if (!value) {
    throw new Error(`Missing required environment variable: ${key}`);
  }
});

// Tạo instance Keycloak với config từ .env
const keycloak = new Keycloak({
  url: requiredEnvVars.VITE_KEYCLOAK_URL,
  realm: requiredEnvVars.VITE_KEYCLOAK_REALM,
  clientId: requiredEnvVars.VITE_KEYCLOAK_CLIENT_ID,
});

export default keycloak;
