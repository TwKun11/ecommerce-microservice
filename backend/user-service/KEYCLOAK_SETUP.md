# Keycloak Setup Guide for User Service

## 1. Start Keycloak Server

```bash
# Download and start Keycloak (if not already running)
# Make sure Keycloak is running on http://localhost:8085
```

## 2. Access Keycloak Admin Console

1. Open browser and go to: `http://localhost:8085`
2. Click "Administration Console"
3. Login with admin credentials

## 3. Create Realm

1. Click on "Master" dropdown (top left)
2. Click "Create Realm"
3. Set Realm name: `user-service`
4. Click "Create"

## 4. Create Client

1. In the `user-service` realm, go to "Clients" in the left menu
2. Click "Create client"
3. Set the following:
   - **Client ID**: `user-service`
   - **Client Type**: `OpenID Connect`
   - **Client authentication**: `ON` (important!)
4. Click "Next"
5. In "Capability config":
   - **Standard flow**: ON
   - **Direct access grants**: ON (important for password grant!)
   - **Service accounts roles**: ON
6. Click "Next"
7. In "Login settings":
   - **Root URL**: `http://localhost:8083`
   - **Home URL**: `http://localhost:8083`
   - **Valid redirect URIs**: `http://localhost:8083/*`
   - **Valid post logout redirect URIs**: `http://localhost:8083/*`
   - **Web origins**: `http://localhost:8083`
8. Click "Save"

## 5. Get Client Secret

1. In the client settings, go to "Credentials" tab
2. Copy the "Client secret" value
3. Update your `application.yml` with this secret:
   ```yaml
   keycloak:
     credentials:
       secret: YOUR_CLIENT_SECRET_HERE
   ```

## 6. Create Realm Roles

1. Go to "Realm roles" in the left menu
2. Click "Create role"
3. Create USER role:
   - **Role name**: `USER`
   - **Description**: `Default user role`
4. Click "Save"
5. Click "Create role" again
6. Create ADMIN role:
   - **Role name**: `ADMIN`
   - **Description**: `Administrator role`
7. Click "Save"

## 7. Configure Client Roles (Optional but Recommended)

1. Go to "Clients" → Select "user-service" client
2. Go to "Roles" tab
3. Click "Create role"
4. Set:
   - **Role name**: `USER`
   - **Description**: `User role for user-service client`
5. Click "Save"

## 8. Configure Default Roles

1. Go to "Realm settings" in the left menu
2. Go to "User registration" tab
3. Set **Default roles**: Add `USER` role
4. Click "Save"

## 8a. Create Admin User

1. Go to "Users" in the left menu
2. Click "Create new user"
3. Set:
   - **Username**: `admin`
   - **Email**: `admin@example.com`
   - **First name**: `Admin`
   - **Last name**: `User`
   - **Email verified**: ON
   - **Enabled**: ON
4. Click "Create"
5. Go to "Credentials" tab
6. Set password and turn off "Temporary"
7. Go to "Role mapping" tab
8. Click "Assign role"
9. Select both "USER" and "ADMIN" roles and assign them

## 9. Test User Creation (Optional)

1. Go to "Users" in the left menu
2. Click "Create new user"
3. Set:
   - **Username**: `testuser`
   - **Email**: `test@example.com`
   - **First name**: `Test`
   - **Last name**: `User`
   - **Email verified**: ON
   - **Enabled**: ON
4. Click "Create"
5. Go to "Credentials" tab
6. Set password and turn off "Temporary"
7. Go to "Role mapping" tab
8. Click "Assign role"
9. Select "USER" role and assign it

## 10. Configure Token Settings

1. Go to "Clients" → Select "user-service" client
2. Go to "Advanced" tab
3. Set:
   - **Access Token Lifespan**: 30 minutes (or as needed)
   - **Client Session Idle**: 30 minutes
   - **Client Session Max**: 12 hours

## 11. Enable Password Grant Type

1. In client settings, ensure "Direct access grants" is ON
2. In "Service account roles" tab, ensure proper roles are assigned

## 11a. Configure Google Identity Provider (Optional)

1. Go to "Identity providers" in the left menu
2. Click "Add provider" → Select "Google"
3. Set:
   - **Client ID**: Your Google OAuth Client ID
   - **Client Secret**: Your Google OAuth Client Secret
4. Click "Save"
5. In "Mappers" tab, add mappers for:
   - Email → email
   - First Name → given_name
   - Last Name → family_name
6. Go to "Identity provider" → "Google" → "Settings"
7. Set **Default Scopes**: `openid profile email`

## 12. Verify Configuration

Your final `application.yml` should look like this:

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://localhost:8085/realms/user-service

keycloak:
  auth-server-url: http://localhost:8085
  realm: user-service
  resource: user-service
  credentials:
    secret: YOUR_ACTUAL_CLIENT_SECRET
  public-client: false
  principal-attribute: preferred-username
  ssl-required: external
  use-resource-role-mappings: true
```

## 13. Test the Setup

1. Start your Spring Boot application
2. Test registration endpoint:

   ```bash
   curl -X POST http://localhost:8083/api/auth/register \
     -H "Content-Type: application/json" \
     -d '{
       "username": "newuser",
       "email": "newuser@example.com",
       "password": "password123",
       "firstName": "New",
       "lastName": "User"
     }'
   ```

3. Test login endpoint:

   ```bash
   curl -X POST http://localhost:8083/api/auth/login \
     -H "Content-Type: application/json" \
     -d '{
       "username": "newuser",
       "password": "password123"
     }'
   ```

4. Test protected endpoint with token:

   ```bash
   curl -X GET http://localhost:8083/api/auth/profile \
     -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
   ```

5. Test Google login:

   ```bash
   curl -X POST http://localhost:8083/api/auth/google-login \
     -H "Content-Type: application/json" \
     -d '{
       "idToken": "GOOGLE_ID_TOKEN",
       "rememberMe": true
     }'
   ```

6. Test password reset request:

   ```bash
   curl -X POST http://localhost:8083/api/auth/reset-password-request \
     -H "Content-Type: application/json" \
     -d '{
       "email": "user@example.com"
     }'
   ```

7. Test password reset:

   ```bash
   curl -X POST http://localhost:8083/api/auth/reset-password \
     -H "Content-Type: application/json" \
     -d '{
       "token": "RESET_TOKEN",
       "newPassword": "newpassword123"
     }'
   ```

8. Test get all users (Admin only):
   ```bash
   curl -X GET http://localhost:8083/api/auth/users \
     -H "Authorization: Bearer ADMIN_ACCESS_TOKEN"
   ```

## Troubleshooting

1. **401 Unauthorized**: Check if client secret is correct
2. **No roles in token**: Ensure USER role is created and assigned to users
3. **Invalid issuer**: Make sure realm name matches in application.yml
4. **Client not found**: Verify client ID matches in configuration

## Common Issues and Solutions

### Issue: Token doesn't contain roles

**Solution**:

1. Go to Clients → user-service → Client scopes
2. Click on "user-service-dedicated"
3. Go to "Mappers" tab
4. Ensure "realm roles" and "client roles" mappers are present
5. If not, create them:
   - Mapper Type: "User Realm Role"
   - Name: "realm roles"
   - Token Claim Name: "realm_access.roles"
   - Claim JSON Type: String
   - Add to ID token: ON
   - Add to access token: ON

### Issue: Registration fails

**Solution**: Ensure the USER role exists in the realm and the service has admin privileges.

### Issue: Login returns 401

**Solution**:

1. Check client secret
2. Ensure "Direct access grants" is enabled
3. Verify realm name matches configuration
