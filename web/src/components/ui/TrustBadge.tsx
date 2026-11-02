import type { ReactNode } from "react";

/** How far a figure can be trusted. Every price on the site carries one. */
export type Trust = "verified" | "stale" | "excluded";

const TRUST_CLASSES: Record<Trust, string> = {
  verified: "bg-soft text-soft-ink",
  stale: "bg-caution-soft text-caution",
  excluded: "bg-sunk text-muted",
};

export function TrustBadge({ trust, children }: { trust: Trust; children: ReactNode }) {
  return (
    <span
      className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-[13px] font-semibold ${TRUST_CLASSES[trust]}`}
    >
      {children}
    </span>
  );
}
