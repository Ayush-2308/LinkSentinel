import { Link, Navigate, Route, Routes, useNavigate } from "react-router-dom";
import { getToken, setToken } from "./api";
import LinksPage from "./pages/LinksPage";
import LoginPage from "./pages/LoginPage";
import StatsPage from "./pages/StatsPage";

function RequireAuth({ children }) {
  if (!getToken()) return <Navigate to="/login" replace />;
  return children;
}

function Frame({ children }) {
  const navigate = useNavigate();

  function signOut() {
    setToken(null);
    navigate("/login");
  }

  return (
    <div className="min-h-screen">
      <header className="border-b border-line bg-white">
        <div className="mx-auto flex max-w-3xl items-center justify-between px-4 py-4">
          <Link to="/" className="text-lg font-semibold tracking-tight">
            LinkSentinel
          </Link>
          {getToken() ? (
            <button type="button" onClick={signOut} className="text-sm text-ink-soft">
              Sign out
            </button>
          ) : null}
        </div>
      </header>
      <main className="mx-auto max-w-3xl px-4 py-8">{children}</main>
    </div>
  );
}

export default function App() {
  return (
    <Routes>
      <Route
        path="/login"
        element={
          <Frame>
            <LoginPage />
          </Frame>
        }
      />
      <Route
        path="/"
        element={
          <RequireAuth>
            <Frame>
              <LinksPage />
            </Frame>
          </RequireAuth>
        }
      />
      <Route
        path="/links/:code"
        element={
          <RequireAuth>
            <Frame>
              <StatsPage />
            </Frame>
          </RequireAuth>
        }
      />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
