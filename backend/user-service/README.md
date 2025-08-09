# User Service - Authentication với Keycloak

Microservice xử lý authentication và user management sử dụng Keycloak.

## Tính năng

- ✅ **Đăng ký tài khoản** - Tạo user mới trong Keycloak
- ✅ **Đăng nhập** - Xác thực và lấy JWT token
- ✅ **Xem profile** - Lấy thông tin người dùng từ JWT
- ✅ **Quên mật khẩu** - Gửi email reset password
- ✅ **Đổi mật khẩu** - Cập nhật password cho user đã đăng nhập

## Cấu hình Keycloak

### 1. Khởi động Keycloak

```bash
docker run -p 8080:8080 -e KEYCLOAK_ADMIN=admin -e KEYCLOAK_ADMIN_PASSWORD=admin quay.io/keycloak/keycloak:23.0.0 start-dev
```

### 2. Tạo Realm và Client

1. Truy cập: http://localhost:8080/admin
2. Đăng nhập: admin/admin
3. Tạo Realm: `ecommerce-realm`
4. Tạo Client: `ecommerce-app`
   - Client ID: `ecommerce-app`
   - Client authentication: ON
   - Authorization: OFF
   - Standard flow: ON
   - Direct access grants: ON

### 3. Tạo Roles

- Tạo realm role: `USER`
- Tạo realm role: `ADMIN`

### 4. Cấu hình Email (cho Forgot Password)

- Realm Settings → Email
- Cấu hình SMTP server

## API Endpoints

### 1. Đăng ký tài khoản

```http
POST /api/auth/register
Content-Type: application/json

{
    "username": "testuser",
    "email": "test@example.com",
    "password": "password123"
}
```

**Response:**

```json
{
  "success": true,
  "message": "User registered successfully"
}
```

### 2. Đăng nhập

```http
POST /api/auth/login
Content-Type: application/json

{
    "username": "testuser",
    "password": "password123"
}
```

**Response:**

```json
{
  "accessToken": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expiresIn": 3600,
  "tokenType": "Bearer"
}
```

### 3. Xem profile (yêu cầu authentication)

```http
GET /api/auth/profile
Authorization: Bearer <access_token>
```

**Response:**

```json
{
  "success": true,
  "message": "Profile retrieved successfully",
  "data": {
    "id": "user-uuid",
    "username": "testuser",
    "email": "test@example.com",
    "firstName": null,
    "lastName": null,
    "emailVerified": false,
    "enabled": true
  }
}
```

### 4. Đổi mật khẩu (yêu cầu authentication)

```http
POST /api/auth/change-password
Authorization: Bearer <access_token>
Content-Type: application/json

{
    "currentPassword": "oldpassword",
    "newPassword": "newpassword123"
}
```

**Response:**

```json
{
  "success": true,
  "message": "Password changed successfully"
}
```

### 5. Quên mật khẩu

```http
POST /api/auth/forgot-password
Content-Type: application/json

{
    "email": "test@example.com"
}
```

**Response:**

```json
{
  "success": true,
  "message": "Password reset email sent successfully"
}
```

## Cấu hình Application

### application.yml

```yaml
server:
  port: 8083

spring:
  application:
    name: user-service
  datasource:
    url: jdbc:postgresql://localhost:5432/userdb
    username: postgres
    password: 123456
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://localhost:8080/realms/ecommerce-realm

keycloak:
  auth-server-url: http://localhost:8080
  realm: ecommerce-realm
  resource: ecommerce-app
  credentials:
    secret: YOUR_CLIENT_SECRET # Lấy từ tab Credentials của Client
  public-client: false
  principal-attribute: preferred-username
  ssl-required: external
  use-resource-role-mappings: true

eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
```

## Dependencies (pom.xml)

```xml
<!-- Security & OAuth2 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-oauth2-jose</artifactId>
</dependency>

<!-- Keycloak Admin Client -->
<dependency>
    <groupId>org.keycloak</groupId>
    <artifactId>keycloak-admin-client</artifactId>
    <version>23.0.0</version>
</dependency>

<!-- HTTP Client for Keycloak -->
<dependency>
    <groupId>org.apache.httpcomponents</groupId>
    <artifactId>httpclient</artifactId>
</dependency>
```

## Cách sử dụng JWT Token

1. **Login** để lấy access token
2. **Gửi token** trong header Authorization: `Bearer <access_token>`
3. **Token sẽ expire** sau thời gian được cấu hình (mặc định 5 phút)
4. **Refresh token** để lấy access token mới (nếu cần)

## Security Configuration

- **Public endpoints**: `/api/auth/register`, `/api/auth/login`, `/api/auth/forgot-password`
- **Protected endpoints**: `/api/auth/profile`, `/api/auth/change-password`
- **JWT validation**: Tự động validate token với Keycloak
- **Role mapping**: Extract roles từ JWT token

## Lưu ý

1. **Client Secret**: Phải lấy từ Keycloak Admin Console
2. **Email Configuration**: Cần cấu hình SMTP để forgot password hoạt động
3. **Database**: Service sử dụng PostgreSQL để lưu metadata (nếu cần)
4. **Roles**: User mới sẽ được gán role `USER` mặc định

## Testing

Sử dụng Postman hoặc curl để test các endpoints. Nhớ include JWT token trong Authorization header cho các protected endpoints.



