import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { api } from "../api";
import ClickChart from "../components/ClickChart";
import VerdictBadge from "../components/VerdictBadge";

export default function StatsPage() {
  const { code } = useParams();
  const [stats, setStats] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    let active = true;
    api("/api/links/" + code + "/stats")
      .then((data) => {
        if (active) setStats(data);
      })
      .catch((err) => {
        if (active) setError(err);
      });
    return () => {
      active = false;
    };
  }, [code]);

  return (
    <div className="space-y-6">
      <Link to="/" className="text-sm text-accent">
        Back to links
      </Link>
      {error ? (
        <div role="alert" className="rounded-md bg-unsafe-bg px-3 py-2 text-sm text-unsafe">
          {error.message}
        </div>
      ) : null}
      {stats === null && !error ? <p className="text-sm text-ink-soft">Loading analytics.</p> : null}
      {stats ? (
        <>
          <section className="rounded-lg border border-line bg-white p-5">
            <div className="flex flex-wrap items-start justify-between gap-3">
              <div>
                <h1 className="text-xl font-semibold">{stats.screening?.title || stats.code}</h1>
                <p className="mt-1 break-all text-sm text-ink-soft">{stats.url}</p>
              </div>
              <VerdictBadge screening={stats.screening} />
            </div>
            <p className="mt-3 text-sm">{stats.screening?.summary}</p>
            <p className="mt-1 text-sm text-ink-soft">{stats.screening?.reason}</p>
            <p className="mt-4 text-sm">
              {stats.clicks} {stats.clicks === 1 ? "click" : "clicks"}
            </p>
          </section>
          <section className="rounded-lg border border-line bg-white p-5">
            <h2 className="mb-3 font-medium">Clicks by day</h2>
            <ClickChart days={stats.clicksByDay} />
          </section>
          <section className="grid gap-4 sm:grid-cols-2">
            <CountList title="Referrers" rows={stats.referrers} empty="No referrers yet." />
            <CountList title="Devices" rows={stats.devices} empty="No devices yet." />
          </section>
        </>
      ) : null}
    </div>
  );
}

function CountList({ title, rows, empty }) {
  return (
    <div className="rounded-lg border border-line bg-white p-5">
      <h2 className="font-medium">{title}</h2>
      {rows?.length ? (
        <ul className="mt-3 space-y-2 text-sm">
          {rows.map((row) => (
            <li key={row.label} className="flex items-center justify-between gap-3">
              <span className="break-all">{row.label}</span>
              <span className="text-ink-soft">{row.count}</span>
            </li>
          ))}
        </ul>
      ) : (
        <p className="mt-3 text-sm text-ink-soft">{empty}</p>
      )}
    </div>
  );
}
