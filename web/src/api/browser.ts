import type { DestinationPage, DestinationSummary, Hub, PlanRequestBody, PlanResponse } from "./types";

/**
 * Calls the API from the visitor's browser. The addresses are relative: in
 * production the CDN sends /api to the API, and locally Next.js forwards it.
 */
export type PlanFailure = "notReady" | "tooMany" | "invalid" | "failed" | "offline";

export type PlanResult = { ok: true; publicId: string } | { ok: false; reason: PlanFailure };

export async function createPlan(body: PlanRequestBody): Promise<PlanResult> {
  let response: Response;
  try {
    response = await fetch("/api/v1/plans", {
      method: "POST",
      headers: { "content-type": "application/json", accept: "application/json" },
      body: JSON.stringify(body),
    });
  } catch {
    return { ok: false, reason: "offline" };
  }

  if (response.ok) {
    const plan = (await response.json()) as PlanResponse;
    return plan.publicId ? { ok: true, publicId: plan.publicId } : { ok: false, reason: "failed" };
  }
  return { ok: false, reason: failureFor(response.status) };
}

function failureFor(status: number): PlanFailure {
  switch (status) {
    case 404:
      return "notReady";
    case 429:
      return "tooMany";
    case 400:
      return "invalid";
    default:
      return "failed";
  }
}

export async function searchDestinations(
  query: string,
  signal: AbortSignal,
): Promise<DestinationSummary[]> {
  const response = await fetch(`/api/v1/destinations?limit=8&q=${encodeURIComponent(query)}`, {
    signal,
    headers: { accept: "application/json" },
  });
  if (!response.ok) {
    return [];
  }
  return ((await response.json()) as DestinationPage).items;
}

export async function fetchHubs(): Promise<Hub[]> {
  const response = await fetch("/api/v1/hubs", { headers: { accept: "application/json" } });
  return response.ok ? ((await response.json()) as Hub[]) : [];
}
