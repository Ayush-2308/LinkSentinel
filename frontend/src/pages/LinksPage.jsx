import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { api } from "../api";
import VerdictBadge from "../components/VerdictBadge";

const emptyForm = { url: "", alias: "", expiresAt: "" };

export default function LinksPage() {
  const [form, setForm] = useState(emptyForm);
  const [links, setLinks] = useState(null);
  const [error, setError] = useState(null);
  const [pending, setPending] = useState(false);
  const [copied, setCopied] = useState("");

  async function load() {
    setError(null);
    try {
      setLinks(await api("/api/links"));
    } catch (err) {
      setLinks([]);
      setError(err);
    }
  }

  useEffect(() => {
    load();
  }, []);

  async function onSubmit(event) {
    event.preventDefault();
    setPending(true);
    setError(null);
    const body = { url: form.url };
    if (form.alias.trim()) body.alias = form.alias.trim();
    if (form.expiresAt) body.expiresAt = new Date(form.expiresAt).toISOString();
    try {
      await api("/api/links", { method: "POST", body: JSON.stringify(body) });
      setForm(emptyForm);
      await load();
    } catch (err) {
      setError(err);
    } finally {
      setPending(false);
    }
  }

  async function remove(code) {
    setError(null);
    try {
      await api("/api/links/" + code, { method: "DELETE" });
      await load();
    } catch (err) {
      setError(err);
    }
  }

  async function copy(link) {
    try {
      await navigator.clipboard.writeText(link.shortUrl);
      setCopied(link.code);
    } catch {
      setCopied("");
    }
  }

  return (
    <div className="space-y-8">
      <section className="rounded-lg border border-line bg-white p-5">
        <h1 className="text-xl font-semibold">New link</h1>
        <form className="mt-4 space-y-4" onSubmit={onSubmit}>
          {error ? <Alert error={error} /> : null}
          <label className="block text-sm">
            URL
            <input
              required
              type="url"
              value={form.url}
              onChange={(event) => setForm({ ...form, url: event.target.value })}
              placeholder="https://example.com/docs"
              className="mt-1 w-full rounded-md border border-line px-3 py-2"
            />
            {error?.fields?.url ? <p className="mt-1 text-unsafe">{error.fields.url}</p> : null}
          </label>
          <div className="grid gap-4 sm:grid-cols-2">
            <label className="block text-sm">
              Alias, optional
              <input
                value={form.alias}
                onChange={(event) => setForm({ ...form, alias: event.target.value })}
                className="mt-1 w-full rounded-md border border-line px-3 py-2"
              />
              {error?.fields?.alias ? <p className="mt-1 text-unsafe">{error.fields.alias}</p> : null}
            </label>
            <label className="block text-sm">
              Expires, optional
              <input
                type="datetime-local"
                value={form.expiresAt}
                onChange={(event) => setForm({ ...form, expiresAt: event.target.value })}
                className="mt-1 w-full rounded-md border border-line px-3 py-2"
              />
            </label>
          </div>
          <button
            type="submit"
            disabled={pending}
            className="rounded-md bg-accent px-4 py-2 text-sm font-medium text-white disabled:opacity-60"
          >
            {pending ? "Shortening" : "Shorten"}
          </button>
        </form>
      </section>

      <section>
        <h2 className="text-lg font-semibold">Your links</h2>
        {links === null ? <p className="mt-3 text-sm text-ink-soft">Loading your links.</p> : null}
        {links?.length === 0 ? <p className="mt-3 text-sm text-ink-soft">No links yet. Add one above.</p> : null}
        <ul className="mt-3 space-y-3">
          {links?.map((link) => (
            <li key={link.code} className="rounded-lg border border-line bg-white p-4">
              <div className="flex flex-wrap items-start justify-between gap-2">
                <div>
                  <p className="font-medium">{link.screening?.title || link.url}</p>
                  <p className="mt-1 break-all text-sm text-accent">{link.shortUrl}</p>
                  <p className="mt-1 break-all text-sm text-ink-soft">{link.url}</p>
                  <p className="mt-1 text-sm text-ink-soft">{link.screening?.summary}</p>
                </div>
                <VerdictBadge screening={link.screening} />
              </div>
              <div className="mt-3 flex flex-wrap gap-3 text-sm">
                <button type="button" onClick={() => copy(link)} className="text-accent">
                  {copied === link.code ? "Copied" : "Copy"}
                </button>
                <Link to={"/links/" + link.code} className="text-accent">
                  Analytics
                </Link>
                <button type="button" onClick={() => remove(link.code)} className="text-unsafe">
                  Delete
                </button>
              </div>
            </li>
          ))}
        </ul>
      </section>
    </div>
  );
}

function Alert({ error }) {
  return (
    <div role="alert" className="rounded-md bg-unsafe-bg px-3 py-2 text-sm text-unsafe">
      <p>{error.message}</p>
      {error.retryAfter ? <p>Try again in {error.retryAfter} seconds.</p> : null}
    </div>
  );
}
