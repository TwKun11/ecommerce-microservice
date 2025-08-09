# So Sánh Hai Cách Tiếp Cận Authentication

## 🔄 Approach 1: Custom Implementation (Hiện tại)

### Endpoints:

```bash
# Registration
POST /api/auth/register
{
  "username": "user123",
  "email": "user@example.com",
  "password": "password123",
  "firstName": "John",
  "lastName": "Doe"
}

# Login
POST /api/auth/login
{
  "username": "user123",
  "password": "password123",
  "rememberMe": true
}

# Google Login
POST /api/auth/google-login
{
  "idToken": "GOOGLE_ID_TOKEN",
  "rememberMe": true
}

# Password Reset Request
POST /api/auth/reset-password-request
{
  "email": "user@example.com"
}

# Reset Password
POST /api/auth/reset-password
{
  "token": "RESET_TOKEN",
  "newPassword": "newpassword123"
}

# Get Profile
GET /api/auth/profile
Authorization: Bearer JWT_TOKEN

# Change Password
POST /api/auth/change-password
Authorization: Bearer JWT_TOKEN
{
  "oldPassword": "old123",
  "newPassword": "new123"
}

# Get All Users (Admin only)
GET /api/auth/users
Authorization: Bearer ADMIN_JWT_TOKEN
```

### Ưu điểm:

- ✅ Full control over API responses
- ✅ Custom business logic
- ✅ RESTful JSON APIs
- ✅ Easy frontend integration

### Nhược điểm:

- ❌ Nhiều code phải maintain
- ❌ Có thể có security issues
- ❌ Duplicate với Keycloak features

---

## 🚀 Approach 2: Keycloak Native (Khuyến nghị)

### Endpoints:

```bash
# Get Login URL
GET /api/simple-auth/login
Response: {
  "loginUrl": "/oauth2/authorization/keycloak",
  "keycloakDirectUrl": "http://localhost:8085/realms/user-service/protocol/openid-connect/auth?..."
}

# Get Registration URL
GET /api/simple-auth/register
Response: {
  "registerUrl": "http://localhost:8085/realms/user-service/protocol/openid-connect/registrations?..."
}

# Get Forgot Password URL
GET /api/simple-auth/forgot-password
Response: {
  "forgotPasswordUrl": "http://localhost:8085/realms/user-service/login-actions/reset-credentials?..."
}

# Get Account Management URLs
GET /api/simple-auth/account
Response: {
  "accountUrl": "http://localhost:8085/realms/user-service/account/",
  "changePasswordUrl": "http://localhost:8085/realms/user-service/account/password",
  "profileUrl": "http://localhost:8085/realms/user-service/account/personal-info"
}

# Get Current User Info
GET /api/simple-auth/user
Authorization: Bearer JWT_TOKEN
Response: {
  "id": "user-id",
  "username": "user123",
  "email": "user@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "roles": ["USER"],
  "realmAccess": {...},
  "resourceAccess": {...}
}

# Get Logout URL
POST /api/simple-auth/logout
Response: {
  "logoutUrl": "http://localhost:8085/realms/user-service/protocol/openid-connect/logout?..."
}

# Admin Info (Admin only)
GET /api/simple-auth/admin/users
Authorization: Bearer ADMIN_JWT_TOKEN
Response: {
  "adminConsoleUrl": "http://localhost:8085/admin/user-service/console/#/user-service/users",
  "usersApiUrl": "http://localhost:8085/admin/realms/user-service/users",
  "message": "Use Keycloak Admin Console or Admin API to manage users"
}
```

### Ưu điểm:

- ✅ Ít code hơn nhiều
- ✅ Security được đảm bảo bởi Keycloak
- ✅ UI/UX đẹp có sẵn
- ✅ Standards compliant (OAuth2, OpenID Connect)
- ✅ Remember me built-in
- ✅ Google login built-in
- ✅ Admin console đầy đủ

### Nhược điểm:

- ❌ Ít control over UI
- ❌ Phải redirect users ra Keycloak

---

## 🔧 Keycloak Configuration Cần Thiết

### 1. Identity Providers (Google Login):

```
Admin Console → Identity Providers → Google
Client ID: YOUR_GOOGLE_CLIENT_ID
Client Secret: YOUR_GOOGLE_CLIENT_SECRET
```

### 2. Email Settings (Password Reset):

```
Realm Settings → Email
Host: smtp.gmail.com
Port: 587
From: your-email@gmail.com
Username: your-email@gmail.com
Password: your-app-password
```

### 3. Remember Me:

```
Realm Settings → Login
Remember Me: ON

Realm Settings → Sessions
Remember Me Session Idle: 30 days
Remember Me Session Max: 365 days
```

### 4. Roles:

```
Realm Roles:
- USER (default role)
- ADMIN (for admin functions)
```

---

## 📱 Frontend Integration Examples

### Approach 1 (Custom APIs):

```javascript
// Login
const login = async (username, password, rememberMe) => {
  const response = await fetch("/api/auth/login", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ username, password, rememberMe }),
  });
  const data = await response.json();
  localStorage.setItem("token", data.accessToken);
};

// Get Profile
const getProfile = async () => {
  const token = localStorage.getItem("token");
  const response = await fetch("/api/auth/profile", {
    headers: { Authorization: `Bearer ${token}` },
  });
  return response.json();
};
```

### Approach 2 (Keycloak Native):

```javascript
// Get login URL and redirect
const login = async () => {
  const response = await fetch("/api/simple-auth/login");
  const data = await response.json();
  window.location.href = data.loginUrl; // Redirect to Keycloak
};

// After successful login, get user info
const getUser = async () => {
  const response = await fetch("/api/simple-auth/user", {
    headers: { Authorization: `Bearer ${token}` },
  });
  return response.json();
};

// Logout
const logout = async () => {
  const response = await fetch("/api/simple-auth/logout", { method: "POST" });
  const data = await response.json();
  window.location.href = data.logoutUrl; // Redirect to Keycloak logout
};
```

---

## 🎯 Khuyến Nghị

### Cho Development/Prototype:

- Dùng **Approach 2** (Keycloak Native)
- Nhanh, ít bug, secure

### Cho Production với Custom Requirements:

- Dùng **Hybrid**:
  - Keycloak cho authentication flow
  - Custom APIs cho business logic
  - Keep both approaches available

### Migration Strategy:

1. Keep existing custom endpoints
2. Add Keycloak native endpoints
3. Test both approaches
4. Gradually migrate to preferred approach
5. Deprecate unused endpoints

---

## 🚀 Quick Start

### Option 1: Use Custom APIs (Current)

```bash
# Test custom login
curl -X POST http://localhost:8083/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"user","password":"pass","rememberMe":true}'
```

### Option 2: Use Keycloak Native

```bash
# Get login URL
curl http://localhost:8083/api/simple-auth/login

# Then redirect user to the returned loginUrl
# After login, user will be redirected back with JWT token
```

Bạn thích dùng cách nào?
