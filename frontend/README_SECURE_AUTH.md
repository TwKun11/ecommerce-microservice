# 🔐 SECURE AUTHENTICATION SYSTEM - BIG TECH STANDARD

## 📋 Tổng Quan

Hệ thống authentication mới theo chuẩn **Big Tech** với **Authorization Code Flow**:

- ✅ **Backend-only token handling** - Frontend không bao giờ touch refresh token
- ✅ **Memory-only access token** - Không persist anywhere
- ✅ **HttpOnly cookie rotation** - Refresh token rotation mỗi lần dùng
- ✅ **Automatic token refresh** - Proactive và reactive refresh
- ✅ **XSS & CSRF protection** - Complete security

## 🚀 Cách Sử Dụng

### **1. Trong Component**

```tsx
import { useAuth } from "../features/auth/hooks/useAuth";

const MyComponent = () => {
  const { isAuthenticated, loading, login, logout, getTimeUntilExpiry } =
    useAuth();

  if (loading) {
    return <div>Loading...</div>;
  }

  if (!isAuthenticated) {
    return <button onClick={() => login()}>Sign In</button>;
  }

  return (
    <div>
      <p>Welcome! Token expires in {getTimeUntilExpiry()} seconds</p>
      <button onClick={logout}>Logout</button>
    </div>
  );
};
```

### **2. API Calls với Auto-Refresh**

```tsx
import { api } from "../shared/utils/secureApiClient";

const MyComponent = () => {
  const fetchData = async () => {
    try {
      // Token sẽ được tự động inject và refresh nếu cần
      const response = await api.get("/api/protected-endpoint");
      console.log(response.data);
    } catch (error) {
      console.error("API call failed:", error);
    }
  };

  return <button onClick={fetchData}>Fetch Data</button>;
};
```

## 🔄 Flow Hoạt Động

### **1. Login Flow**

```
User clicks Login → Redirect to Keycloak → User authenticates →
Backend exchanges code for tokens → HttpOnly cookie set →
Access token in URL fragment → Frontend extracts to memory
```

### **2. API Call Flow**

```
Frontend makes API call → Axios interceptor adds token →
Backend validates → If expired → Auto refresh with HttpOnly cookie →
Retry original request → Return response
```

### **3. Token Refresh Flow**

```
Token near expiry → Proactive refresh timer →
Backend refresh endpoint → New access token →
Update memory → Continue seamlessly
```

## 📁 File Structure

```
frontend/src/
├── shared/utils/
│   ├── secureTokenManager.ts      # Memory-only token storage
│   └── secureApiClient.ts         # Axios with interceptors
├── features/auth/
│   ├── context/
│   │   └── SecureAuthContext.tsx  # React context
│   └── hooks/
│       └── useAuth.tsx            # Hook for components
```

## 🔧 API Reference

### **useAuth Hook**

```tsx
const {
  isAuthenticated, // boolean - User authentication status
  loading, // boolean - Loading state
  login, // function - Initiate login
  logout, // function - Logout user
  getTimeUntilExpiry, // function - Get seconds until token expires
} = useAuth();
```

### **API Client**

```tsx
import { api } from "../shared/utils/secureApiClient";

// All methods automatically handle authentication
api.get("/endpoint");
api.post("/endpoint", data);
api.put("/endpoint", data);
api.delete("/endpoint");
api.patch("/endpoint", data);
```

## 🔐 Security Features

### **1. XSS Protection**

- ✅ Refresh token trong HttpOnly cookie
- ✅ Access token chỉ trong memory
- ✅ Không persist tokens anywhere

### **2. CSRF Protection**

- ✅ SameSite=Strict cookies
- ✅ Proper CORS configuration
- ✅ State parameter validation

### **3. Token Security**

- ✅ Automatic token rotation
- ✅ Proactive refresh (2 minutes before expiry)
- ✅ Reactive refresh (on 401 errors)
- ✅ Memory-only storage

## 🧪 Testing

### **1. Test Login**

```bash
# 1. Click login button
# 2. Should redirect to Keycloak
# 3. Login with credentials
# 4. Should redirect back with access token
# 5. Check HttpOnly cookie is set
```

### **2. Test API Calls**

```bash
# 1. Make API call
# 2. Check Authorization header is set
# 3. If token expired, should auto-refresh
# 4. Original request should succeed
```

### **3. Test Logout**

```bash
# 1. Click logout
# 2. HttpOnly cookie should be cleared
# 3. Memory token should be cleared
# 4. Should redirect to login
```

## 🎯 Migration từ AuthContext Cũ

### **Thay đổi API:**

| **Old API**     | **New API**            | **Description**       |
| --------------- | ---------------------- | --------------------- |
| `authenticated` | `isAuthenticated`      | Authentication status |
| `token`         | `getTimeUntilExpiry()` | Token info            |
| `login()`       | `login()`              | Same function         |
| `logout()`      | `logout()`             | Same function         |

### **Ví dụ Migration:**

```tsx
// OLD
const { authenticated, token, login, logout } = useAuth();
if (!authenticated) return <LoginButton />;
console.log("Token:", token);

// NEW
const { isAuthenticated, getTimeUntilExpiry, login, logout } = useAuth();
if (!isAuthenticated) return <LoginButton />;
console.log("Token expires in:", getTimeUntilExpiry(), "seconds");
```

## 🏆 Big Tech Compliance

Hệ thống này **100% chuẩn Big Tech** vì:

1. **Backend-only token handling** - Frontend không bao giờ touch refresh token
2. **Memory-only access token** - Không persist anywhere
3. **HttpOnly cookie rotation** - Refresh token rotation mỗi lần dùng
4. **Proper error handling** - Graceful fallback và retry
5. **Clean separation** - Frontend chỉ lo UI, backend lo security

**Approach này giống hệt cách Google, Netflix, Facebook implement! 🚀**
