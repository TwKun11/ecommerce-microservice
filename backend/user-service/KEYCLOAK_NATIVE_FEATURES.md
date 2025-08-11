# Sử Dụng Tính Năng Built-in Của Keycloak

Keycloak đã hỗ trợ sẵn hầu hết các tính năng bạn cần. Thay vì tự code, hãy sử dụng các tính năng có sẵn:

## 1. 🔐 Google Login (Identity Provider)

### Cấu hình trong Keycloak:

1. **Admin Console** → **Identity Providers**
2. **Add provider** → **Google**
3. Điền thông tin:
   ```
   Client ID: YOUR_GOOGLE_CLIENT_ID
   Client Secret: YOUR_GOOGLE_CLIENT_SECRET
   ```
4. **Mappers** → Add mappers:
   - Email → email
   - Given Name → firstName
   - Family Name → lastName

### Sử dụng:

- User có thể login trực tiếp qua Keycloak UI
- Hoặc redirect đến: `http://localhost:8085/realms/user-service/broker/google/login`

## 2. 🔄 Password Reset (Built-in)

### Cấu hình Email trong Keycloak:

1. **Realm Settings** → **Email**
2. Điền thông tin SMTP:
   ```
   Host: smtp.gmail.com
   Port: 587
   From: your-email@gmail.com
   Username: your-email@gmail.com
   Password: your-app-password
   Enable SSL: ON
   Enable StartTLS: ON
   Enable Authentication: ON
   ```

### Sử dụng:

- **Forgot Password**: `GET http://localhost:8085/realms/user-service/login-actions/reset-credentials`
- Keycloak tự động gửi email reset password
- User click link trong email để reset

## 3. ⏰ Remember Me (Built-in)

### Cấu hình:

1. **Realm Settings** → **Login**
2. **Remember Me**: ON
3. **Login** → **Sessions**
4. Set thời gian:
   ```
   SSO Session Idle: 30 minutes
   SSO Session Max: 10 hours
   Remember Me Session Idle: 30 days
   Remember Me Session Max: 365 days
   ```

### Sử dụng:

- Keycloak login form sẽ có checkbox "Remember me"
- Token sẽ có thời gian sống lâu hơn

## 4. 👥 Admin Management (Built-in)

### Roles đã có sẵn:

- `realm-admin`: Full admin quyền
- `view-users`: Xem users
- `manage-users`: Quản lý users

### Admin Console:

- **Users** → Quản lý tất cả users
- **Roles** → Quản lý roles
- **Sessions** → Xem active sessions

## 5. 🔧 Simplified Spring Boot Integration

Thay vì tự code, chỉ cần cấu hình OAuth2 client:

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://localhost:8085/realms/user-service
      client:
        registration:
          keycloak:
            client-id: user-service
            client-secret: YOUR_CLIENT_SECRET
            authorization-grant-type: authorization_code
            redirect-uri: "{baseUrl}/login/oauth2/code/{registrationId}"
            scope: openid,profile,email
        provider:
          keycloak:
            issuer-uri: http://localhost:8085/realms/user-service
            user-name-attribute: preferred_username
```

## 6. 🎯 Endpoints Keycloak Cung Cấp

### Authentication:

```bash
# Login Form
GET http://localhost:8085/realms/user-service/protocol/openid-connect/auth?client_id=user-service&response_type=code&redirect_uri=http://localhost:8083/callback

# Token Exchange
POST http://localhost:8085/realms/user-service/protocol/openid-connect/token

# User Info
GET http://localhost:8085/realms/user-service/protocol/openid-connect/userinfo
```

### Account Management:

```bash
# User Account Console
GET http://localhost:8085/realms/user-service/account

# Change Password
GET http://localhost:8085/realms/user-service/account/password

# Profile Management
GET http://localhost:8085/realms/user-service/account/personal-info
```

### Admin API:

```bash
# Get All Users (with admin token)
GET http://localhost:8085/admin/realms/user-service/users

# User Details
GET http://localhost:8085/admin/realms/user-service/users/{user-id}

# Update User
PUT http://localhost:8085/admin/realms/user-service/users/{user-id}
```

## 7. 🚀 Simplified Controller

Chỉ cần controller đơn giản:

```java
@RestController
public class AuthController {

    // Redirect to Keycloak login
    @GetMapping("/login")
    public String login() {
        return "redirect:/oauth2/authorization/keycloak";
    }

    // Get current user info
    @GetMapping("/user")
    public ResponseEntity<?> user(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(jwt.getClaims());
    }

    // Logout
    @PostMapping("/logout")
    public String logout() {
        return "redirect:/";
    }
}
```

## 8. ✨ Lợi Ích Khi Dùng Keycloak Built-in

### Ưu điểm:

- ✅ **Ít code hơn**: Không cần tự implement
- ✅ **Bảo mật cao**: Keycloak đã được test kỹ
- ✅ **UI có sẵn**: Login, register, forgot password forms
- ✅ **Quản lý tập trung**: Admin console đầy đủ
- ✅ **Standards compliant**: OAuth2, OpenID Connect
- ✅ **Scalable**: Cluster, caching built-in

### Nhược điểm:

- ❌ **Ít custom**: UI và flow cố định
- ❌ **Learning curve**: Cần học Keycloak config

## 9. 🛠 Migration Plan

Để chuyển sang dùng Keycloak built-in:

1. **Keep existing endpoints** cho backward compatibility
2. **Add Keycloak OAuth2 flow** cho new features
3. **Gradually migrate** users sang Keycloak flow
4. **Remove custom code** khi không cần thiết

## 10. 📋 Next Steps

1. **Configure Identity Providers** (Google, Facebook, etc.)
2. **Setup Email** cho password reset
3. **Configure Remember Me** settings
4. **Test Keycloak flows** thay vì custom endpoints
5. **Update frontend** để dùng Keycloak redirects

Bạn muốn tôi cấu hình cụ thể phần nào trước?

