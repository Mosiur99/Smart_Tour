import type { ReactNode } from "react";

type Tone = "caution" | "problem";

const TONE_CLASSES: Record<Tone, string> = {
  caution: "bg-caution-soft text-caution",
  problem: "bg-problem-soft text-problem",
};

export function Notice({ tone, children }: { tone: Tone; children: ReactNode }) {
  return (
    <p
      role={tone === "problem" ? "alert" : "status"}
      className={`m-0 rounded-card px-4 py-3 ${TONE_CLASSES[tone]}`}
    >
      {children}
    </p>
  );
}
