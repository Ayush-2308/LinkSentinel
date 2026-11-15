const base = import.meta.env.VITE_API_URL ?? "http://localhost:8080";
const tokenKey = "linksentinel.token";

export function getToken() {
  return localStorage.getItem(tokenKey);
}

export function setToken(token) {
  if (token) localStorage.setItem(tokenKey, token);
  else localStorage.removeItem(tokenKey);
}

export async function api(path, options = {}) {
  const headers = { ...(options.headers ?? {}) };
  if (options.body) headers["Content-Type"] = "application/json";
  const token = getToken();
  if (token) headers.Authorization = `Bearer ${token}`;

  let response;
  try {
    response = await fetch(base + path, { ...options, headers });
  } catch {
    const error = new Error("Could not reach the server.");
    error.fields = {};
    throw error;
  }

  const text = await response.text();
  let data = null;
  if (text) {
    try {
      data = JSON.parse(text);
    } catch {
      const error = new Error("The server returned an unexpected response.");
      error.status = response.status;
      error.fields = {};
      throw error;
    }
  }

  if (!response.ok) {
    const error = new Error(data?.message || "Request failed");
    error.status = response.status;
    error.fields = data?.fields ?? {};
    error.retryAfter = response.headers.get("Retry-After");
    throw error;
  }
  return data;
}
