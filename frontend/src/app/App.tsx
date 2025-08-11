import React, { Suspense, lazy } from "react";
import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import { SecureAuthProvider } from "../features/auth/context/SecureAuthContext";

// Lazy load components
const HomePage = lazy(() =>
  import("../features/dashboard/pages/HomePage").then((module) => ({
    default: module.default,
  }))
);

// Loading Spinner Component
const LoadingSpinner: React.FC<{ text?: string }> = ({
  text = "Loading...",
}) => (
  <div
    style={{
      display: "flex",
      flexDirection: "column",
      alignItems: "center",
      justifyContent: "center",
      minHeight: "100vh",
      backgroundColor: "#f8fafc",
      gap: "16px",
    }}
  >
    <div
      style={{
        width: "40px",
        height: "40px",
        border: "4px solid #e2e8f0",
        borderTop: "4px solid #667eea",
        borderRadius: "50%",
        animation: "spin 1s linear infinite",
      }}
    />
    <p style={{ color: "#666", fontSize: "16px", margin: 0 }}>{text}</p>
    <style>
      {`
        @keyframes spin {
          0% { transform: rotate(0deg); }
          100% { transform: rotate(360deg); }
        }
      `}
    </style>
  </div>
);

// Error Boundary Component
class ErrorBoundary extends React.Component<
  { children: React.ReactNode },
  { hasError: boolean; error?: Error }
> {
  constructor(props: { children: React.ReactNode }) {
    super(props);
    this.state = { hasError: false };
  }

  static getDerivedStateFromError(error: Error) {
    return { hasError: true, error };
  }

  componentDidCatch(error: Error, errorInfo: React.ErrorInfo) {
    console.error("App Error:", error, errorInfo);
  }

  render() {
    if (this.state.hasError) {
      return (
        <div
          style={{
            padding: "20px",
            textAlign: "center",
            minHeight: "100vh",
            display: "flex",
            flexDirection: "column",
            justifyContent: "center",
            alignItems: "center",
            gap: "20px",
            backgroundColor: "#f8fafc",
          }}
        >
          <h2 style={{ color: "#e53e3e", margin: 0 }}>Something went wrong</h2>
          <p style={{ color: "#666", maxWidth: "400px", margin: 0 }}>
            {this.state.error?.message || "An unexpected error occurred"}
          </p>
          <button
            onClick={() => window.location.reload()}
            style={{
              padding: "12px 24px",
              backgroundColor: "#667eea",
              color: "white",
              border: "none",
              borderRadius: "6px",
              cursor: "pointer",
              fontSize: "16px",
              fontWeight: "600",
            }}
          >
            Reload Page
          </button>
        </div>
      );
    }
    return this.props.children;
  }
}

export const AppRoutes: React.FC = () => (
  <ErrorBoundary>
    <SecureAuthProvider>
      <BrowserRouter>
        <Suspense fallback={<LoadingSpinner text="Loading application..." />}>
          <Routes>
            <Route path="/" element={<HomePage />} />
            <Route path="*" element={<Navigate to="/" replace />} />
          </Routes>
        </Suspense>
      </BrowserRouter>
    </SecureAuthProvider>
  </ErrorBoundary>
);
