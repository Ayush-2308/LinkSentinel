export default function VerdictBadge({ screening }) {
  if (!screening) return null;
  const safe = screening.safe;
  return (
    <span
      className={
        safe
          ? "inline-flex rounded-full bg-safe-bg px-2 py-0.5 text-xs font-medium text-safe"
          : "inline-flex rounded-full bg-unsafe-bg px-2 py-0.5 text-xs font-medium text-unsafe"
      }
    >
      {safe ? "Safe" : "Unsafe"}
    </span>
  );
}
