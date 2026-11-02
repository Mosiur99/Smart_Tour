export function Skeleton({ width = "100%", label }: { width?: string; label?: string }) {
  return (
    <div
      role={label ? "status" : undefined}
      aria-label={label}
      aria-hidden={label ? undefined : true}
      style={{ width }}
      className="h-4 animate-pulse rounded-control bg-sunk"
    />
  );
}
