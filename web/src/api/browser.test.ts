import { afterEach, describe, expect, it, vi } from "vitest";
import { createPlan, searchDestinations } from "./browser";
import type { PlanRequestBody } from "./types";

const BODY: PlanRequestBody = {
  originHub: "dhaka",
  destination: "sylhet",
  days: 3,
  people: 2,
  budget: "MID",
  month: 12,
  interests: [],
};

function answer(status: number, body: unknown = {}) {
  vi.stubGlobal("fetch", vi.fn().mockResolvedValue(new Response(JSON.stringify(body), { status })));
}

afterEach(() => vi.unstubAllGlobals());

describe("createPlan", () => {
  it("returns the plan's public id", async () => {
    answer(201, { publicId: "7W7TmZWry2a6" });
    expect(await createPlan(BODY)).toEqual({ ok: true, publicId: "7W7TmZWry2a6" });
  });

  it.each([
    [404, "notReady"],
    [429, "tooMany"],
    [400, "invalid"],
    [500, "failed"],
    [503, "failed"],
  ])("turns status %i into %s", async (status, reason) => {
    answer(status, { title: "Problem" });
    expect(await createPlan(BODY)).toEqual({ ok: false, reason });
  });

  it("reports when the network is down", async () => {
    vi.stubGlobal("fetch", vi.fn().mockRejectedValue(new TypeError("Failed to fetch")));
    expect(await createPlan(BODY)).toEqual({ ok: false, reason: "offline" });
  });

  it("sends the request as JSON", async () => {
    answer(201, { publicId: "7W7TmZWry2a6" });
    await createPlan(BODY);
    const [url, init] = vi.mocked(fetch).mock.calls[0];
    expect(url).toBe("/api/v1/plans");
    expect(JSON.parse(init?.body as string)).toEqual(BODY);
  });
});

describe("searchDestinations", () => {
  it("encodes the query", async () => {
    answer(200, { items: [], nextCursor: null });
    await searchDestinations("cox's bazar", new AbortController().signal);
    expect(vi.mocked(fetch).mock.calls[0][0]).toBe("/api/v1/destinations?limit=8&q=cox's%20bazar");
  });

  it("returns nothing when the API fails", async () => {
    answer(500);
    expect(await searchDestinations("syl", new AbortController().signal)).toEqual([]);
  });
});
