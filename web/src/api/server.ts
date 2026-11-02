import "server-only";
import type { DestinationDetail, DestinationPage, DestinationSummary, Hub, PlanResponse } from "./types";

/**
 * Reads the API from the Next.js server. Pages call these while rendering, and
 * the results are cached for as long as each kind of data may stay unchanged.
 */
const API_ORIGIN = process.env.API_ORIGIN ?? "http://localhost:8081";

/** Every catalog read carries this tag, so a publish can refresh them all at once. */
export const CATALOG_TAG = "catalog";

const TEN_MINUTES = 600;
const ONE_HOUR = 3600;
const ONE_DAY = 86400;
const SLUG = /^[a-z0-9]+(-[a-z0-9]+)*$/;
const PUBLIC_ID = /^[2-9a-zA-Z]{12}$/;

export class ApiError extends Error {
  constructor(readonly status: number, path: string) {
    super(`The API answered ${status} for ${path}`);
  }
}

/** Returns null for 404, so pages can show their not-found state. */
async function get<T>(path: string, revalidate: number, tags: string[] = [CATALOG_TAG]): Promise<T | null> {
  const response = await fetch(`${API_ORIGIN}${path}`, {
    next: { revalidate, tags },
    headers: { accept: "application/json" },
    signal: AbortSignal.timeout(5000),
  });
  if (response.status === 404) {
    return null;
  }
  if (!response.ok) {
    throw new ApiError(response.status, path);
  }
  return (await response.json()) as T;
}

export async function getHubs(): Promise<Hub[]> {
  return (await get<Hub[]>("/api/v1/hubs", ONE_HOUR)) ?? [];
}

export function getDestination(slug: string): Promise<DestinationDetail | null> {
  // Checked here too, so a crafted address never reaches the API.
  return SLUG.test(slug) ? get(`/api/v1/destinations/${slug}`, TEN_MINUTES) : Promise.resolve(null);
}

/** A saved plan never changes, so it can be cached for a long time. */
export function getPlan(publicId: string): Promise<PlanResponse | null> {
  return PUBLIC_ID.test(publicId) ? get(`/api/v1/plans/${publicId}`, ONE_DAY, []) : Promise.resolve(null);
}

/** Walks every page. There are 64 districts, so this stays small. */
export async function getAllDestinations(): Promise<DestinationSummary[]> {
  const all: DestinationSummary[] = [];
  let cursor: string | null = null;
  for (let page = 0; page < 20; page++) {
    const query: string = cursor ? `&cursor=${encodeURIComponent(cursor)}` : "";
    const result: DestinationPage | null = await get<DestinationPage>(
      `/api/v1/destinations?limit=50${query}`,
      TEN_MINUTES,
    );
    all.push(...(result?.items ?? []));
    cursor = result?.nextCursor ?? null;
    if (!cursor) {
      break;
    }
  }
  return all;
}
