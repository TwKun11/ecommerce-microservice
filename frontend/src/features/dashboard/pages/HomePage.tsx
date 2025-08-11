import React from "react";
import { useAuth } from "../../auth/hooks/useAuth";

const HomePage: React.FC = () => {
  const { login, logout, isAuthenticated, getTimeUntilExpiry } = useAuth();

  if (!isAuthenticated) {
    return (
      <div style={{ minHeight: "100vh", backgroundColor: "#f8fafc" }}>
        {/* Header */}
        <header
          style={{
            backgroundColor: "white",
            padding: "16px 24px",
            boxShadow: "0 2px 4px rgba(0,0,0,0.1)",
            display: "flex",
            justifyContent: "space-between",
            alignItems: "center",
          }}
        >
          <h1
            style={{
              margin: 0,
              color: "#333",
              fontSize: "24px",
              fontWeight: "600",
            }}
          >
            🛒 E-Commerce Platform
          </h1>
          <button
            onClick={() => login()}
            style={{
              padding: "12px 24px",
              backgroundColor: "#667eea",
              color: "white",
              border: "none",
              borderRadius: "8px",
              cursor: "pointer",
              fontSize: "16px",
              fontWeight: "600",
              transition: "background-color 0.3s ease",
            }}
            onMouseEnter={(e) =>
              (e.currentTarget.style.backgroundColor = "#5a6fd8")
            }
            onMouseLeave={(e) =>
              (e.currentTarget.style.backgroundColor = "#667eea")
            }
          >
            Sign In
          </button>
        </header>

        {/* Hero Section */}
        <main
          style={{ padding: "60px 24px", maxWidth: "1200px", margin: "0 auto" }}
        >
          <div style={{ textAlign: "center", marginBottom: "60px" }}>
            <h1
              style={{
                fontSize: "48px",
                fontWeight: "700",
                color: "#333",
                margin: "0 0 20px 0",
                lineHeight: "1.2",
              }}
            >
              Welcome to Our E-Commerce Platform
            </h1>
            <p
              style={{
                fontSize: "20px",
                color: "#666",
                margin: "0 0 40px 0",
                maxWidth: "600px",
                marginLeft: "auto",
                marginRight: "auto",
                lineHeight: "1.6",
              }}
            >
              Discover amazing products, manage your orders, and enjoy a
              seamless shopping experience. Sign in to access your personalized
              dashboard.
            </p>
            <button
              onClick={() => login()}
              style={{
                padding: "16px 32px",
                backgroundColor: "#667eea",
                color: "white",
                border: "none",
                borderRadius: "12px",
                cursor: "pointer",
                fontSize: "18px",
                fontWeight: "600",
                transition: "all 0.3s ease",
              }}
              onMouseEnter={(e) => {
                e.currentTarget.style.backgroundColor = "#5a6fd8";
                e.currentTarget.style.transform = "translateY(-2px)";
              }}
              onMouseLeave={(e) => {
                e.currentTarget.style.backgroundColor = "#667eea";
                e.currentTarget.style.transform = "translateY(0)";
              }}
            >
              Get Started - Sign In
            </button>
          </div>

          {/* Features Grid */}
          <div
            style={{
              display: "grid",
              gridTemplateColumns: "repeat(auto-fit, minmax(300px, 1fr))",
              gap: "32px",
              marginBottom: "60px",
            }}
          >
            {[
              {
                icon: "🛍️",
                title: "Shop Products",
                description:
                  "Browse through thousands of products from trusted sellers",
              },
              {
                icon: "📦",
                title: "Track Orders",
                description:
                  "Monitor your orders in real-time with detailed tracking",
              },
              {
                icon: "💰",
                title: "Secure Payments",
                description:
                  "Multiple payment options with bank-level security",
              },
              {
                icon: "🎯",
                title: "Personalized Experience",
                description: "Get recommendations based on your preferences",
              },
            ].map((feature, index) => (
              <div
                key={index}
                style={{
                  backgroundColor: "white",
                  padding: "32px",
                  borderRadius: "16px",
                  boxShadow: "0 4px 6px rgba(0,0,0,0.1)",
                  textAlign: "center",
                }}
              >
                <div style={{ fontSize: "48px", marginBottom: "16px" }}>
                  {feature.icon}
                </div>
                <h3
                  style={{
                    margin: "0 0 12px 0",
                    color: "#333",
                    fontSize: "20px",
                    fontWeight: "600",
                  }}
                >
                  {feature.title}
                </h3>
                <p
                  style={{
                    margin: 0,
                    color: "#666",
                    fontSize: "16px",
                    lineHeight: "1.5",
                  }}
                >
                  {feature.description}
                </p>
              </div>
            ))}
          </div>
        </main>
      </div>
    );
  }

  // If authenticated, show simple dashboard
  return (
    <div style={{ minHeight: "100vh", backgroundColor: "#f8fafc" }}>
      {/* Header */}
      <header
        style={{
          backgroundColor: "white",
          padding: "16px 24px",
          boxShadow: "0 2px 4px rgba(0,0,0,0.1)",
          display: "flex",
          justifyContent: "space-between",
          alignItems: "center",
        }}
      >
        <h1
          style={{
            margin: 0,
            color: "#333",
            fontSize: "24px",
            fontWeight: "600",
          }}
        >
          🛒 E-Commerce Dashboard
        </h1>
        <div style={{ display: "flex", alignItems: "center", gap: "16px" }}>
          <span style={{ color: "#666", fontSize: "14px" }}>
            Welcome back! 👋
          </span>
          <button
            onClick={logout}
            style={{
              padding: "8px 16px",
              backgroundColor: "#e53e3e",
              color: "white",
              border: "none",
              borderRadius: "6px",
              cursor: "pointer",
              fontSize: "14px",
              fontWeight: "500",
            }}
          >
            Logout
          </button>
        </div>
      </header>

      {/* Main Content */}
      <main style={{ padding: "24px", maxWidth: "1200px", margin: "0 auto" }}>
        <div
          style={{
            backgroundColor: "white",
            padding: "40px",
            borderRadius: "16px",
            boxShadow: "0 4px 6px rgba(0,0,0,0.1)",
            textAlign: "center",
          }}
        >
          <h2
            style={{
              margin: "0 0 20px 0",
              color: "#333",
              fontSize: "28px",
              fontWeight: "600",
            }}
          >
            🎉 Welcome to Your Dashboard!
          </h2>
          <p
            style={{
              fontSize: "18px",
              color: "#666",
              margin: "0 0 30px 0",
              lineHeight: "1.6",
            }}
          >
            You are successfully authenticated with Keycloak. Your session is
            secure and your token is being managed automatically.
          </p>

          <div
            style={{
              backgroundColor: "#f8fafc",
              padding: "20px",
              borderRadius: "12px",
              border: "1px solid #e2e8f0",
              marginBottom: "30px",
            }}
          >
            <h3
              style={{
                margin: "0 0 15px 0",
                color: "#333",
                fontSize: "18px",
                fontWeight: "600",
              }}
            >
              🔐 Security Status
            </h3>
            <div
              style={{
                display: "flex",
                justifyContent: "center",
                gap: "20px",
                flexWrap: "wrap",
              }}
            >
              <div style={{ textAlign: "center" }}>
                <div style={{ fontSize: "24px", marginBottom: "8px" }}>✅</div>
                <div
                  style={{
                    fontSize: "14px",
                    color: "#48bb78",
                    fontWeight: "600",
                  }}
                >
                  Authenticated
                </div>
              </div>
              <div style={{ textAlign: "center" }}>
                <div style={{ fontSize: "24px", marginBottom: "8px" }}>🔄</div>
                <div
                  style={{
                    fontSize: "14px",
                    color: "#667eea",
                    fontWeight: "600",
                  }}
                >
                  Auto Refresh
                </div>
              </div>
              <div style={{ textAlign: "center" }}>
                <div style={{ fontSize: "24px", marginBottom: "8px" }}>🔒</div>
                <div
                  style={{
                    fontSize: "14px",
                    color: "#ed8936",
                    fontWeight: "600",
                  }}
                >
                  Secure Token
                </div>
              </div>
            </div>
          </div>

          <div
            style={{
              display: "grid",
              gridTemplateColumns: "repeat(auto-fit, minmax(250px, 1fr))",
              gap: "20px",
              marginBottom: "30px",
            }}
          >
            {[
              {
                icon: "📊",
                title: "Analytics",
                desc: "View your performance metrics",
              },
              {
                icon: "🛍️",
                title: "Products",
                desc: "Manage your product catalog",
              },
              { icon: "📦", title: "Orders", desc: "Track and manage orders" },
              { icon: "👥", title: "Users", desc: "Manage user accounts" },
            ].map((item, index) => (
              <div
                key={index}
                style={{
                  padding: "24px",
                  backgroundColor: "#f8fafc",
                  borderRadius: "12px",
                  border: "1px solid #e2e8f0",
                  cursor: "pointer",
                  transition: "all 0.3s ease",
                }}
                onMouseEnter={(e) => {
                  e.currentTarget.style.backgroundColor = "#e2e8f0";
                  e.currentTarget.style.transform = "translateY(-2px)";
                }}
                onMouseLeave={(e) => {
                  e.currentTarget.style.backgroundColor = "#f8fafc";
                  e.currentTarget.style.transform = "translateY(0)";
                }}
              >
                <div style={{ fontSize: "32px", marginBottom: "12px" }}>
                  {item.icon}
                </div>
                <h4
                  style={{
                    margin: "0 0 8px 0",
                    color: "#333",
                    fontSize: "16px",
                    fontWeight: "600",
                  }}
                >
                  {item.title}
                </h4>
                <p style={{ margin: 0, color: "#666", fontSize: "14px" }}>
                  {item.desc}
                </p>
              </div>
            ))}
          </div>

          <p style={{ fontSize: "14px", color: "#999", margin: 0 }}>
            Token expires in: {getTimeUntilExpiry()} seconds
          </p>
        </div>
      </main>
    </div>
  );
};

export default HomePage;
