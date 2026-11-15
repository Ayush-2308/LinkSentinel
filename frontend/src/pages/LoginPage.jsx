import { useState } from "react";
import { Navigate, useNavigate } from "react-router-dom";
import { api, getToken, setToken } from "../api";

export default function LoginPage() {
  const navigate = useNavigate();
  const [mode, setMode] = useState("login");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState(null);
  const [pending, setPending] = useState(false);

  if (getToken()) return <Navigate to="/" replace />;

  async function onSubmit(event) {
    event.preventDefault();
    setPending(true);
    setError(null);
    const path = mode === "login" ? "/api/auth/login" : "/api/auth/register";
    try {
      const result = await api(path, {
        method: "POST",
        body: JSON.stringify({ email, password }),
      });
      setToken(result.token);
      navigate("/");
    } catch (err) {
      setError(err);
    } finally {
      setPending(false);
    }
  }

  const submitLabel = pending ? "Please wait" : mode === "login" ? "Sign in" : "Create account";

  return (
    <section className="mx-auto max-w-md rounded-lg border border-line bg-white p-6">
      <h1 className="text-xl font-semibold">{mode === "login" ? "Sign in" : "Create an account"}</h1>
      <p className="mt-1 text-sm text-ink-soft">Shorten a link and see who opens it.</p>
      <form className="mt-6 space-y-4" onSubmit={onSubmit}>
        {error ? <FormError error={error} /> : null}
        <label className="block text-sm">
          Email
          <input
            type="email"
            required
            autoComplete="email"
            value={email}
            onChange={(event) => setEmail(event.target.value)}
            className="mt-1 w-full rounded-md border border-line px-3 py-2"
          />
        </label>
        <label className="block text-sm">
          Password
          <input
            type="password"
            required
            minLength={8}
            autoComplete={mode === "login" ? "current-password" : "new-password"}
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            className="mt-1 w-full rounded-md border border-line px-3 py-2"
          />
        </label>
        <button
          type="submit"
          disabled={pending}
          className="w-full rounded-md bg-accent px-4 py-2 text-sm font-medium text-white disabled:opacity-60"
        >
          {submitLabel}
        </button>
      </form>
      <button
        type="button"
        className="mt-4 text-sm text-accent"
        onClick={() => {
          setMode(mode === "login" ? "register" : "login");
          setError(null);
        }}
      >
        {mode === "login" ? "Need an account? Create one" : "Already have an account? Sign in"}
      </button>
    </section>
  );
}

function FormError({ error }) {
  const fields = Object.values(error.fields ?? {});
  return (
    <div role="alert" className="rounded-md bg-unsafe-bg px-3 py-2 text-sm text-unsafe">
      <p>{error.message}</p>
      {error.retryAfter ? <p>Try again in {error.retryAfter} seconds.</p> : null}
      {fields.map((message) => (
        <p key={message}>{message}</p>
      ))}
    </div>
  );
}
