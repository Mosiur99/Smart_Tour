import { describe, expect, it, vi } from "vitest";
import { adminRequest } from "./api";

function answer(status: number, body: string) {
  const fetchMock = vi.fn().mockResolvedValue(new Response(body, { status }));
  vi.stubGlobal("fetch", fetchMock);
  return fetchMock;
}

describe("adminRequest", () => {
  it("returns the data and never uses a cached answer", async () => {
    const fetchMock = answer(200, JSON.stringify({ email: "owner@example.com", role: "PUBLISHER" }));

    expect(await adminRequest("/me")).toEqual({ ok: true, data: { email: "owner@example.com", role: "PUBLISHER" } });
    expect(fetchMock).toHaveBeenCalledWith("/api/v1/admin/me", expect.objectContaining({ method: "GET", cache: "no-store" }));
  });

  it("sends changes as JSON", async () => {
    const fetchMock = answer(200, "{}");

    await adminRequest("/hotels/7", "PUT", { revision: 3, values: { priceMin: 1800 } });

    const [, init] = fetchMock.mock.calls[0] as [string, RequestInit];
    expect(init.method).toBe("PUT");
    expect(init.headers).toMatchObject({ "content-type": "application/json" });
    expect(JSON.parse(init.body as string)).toEqual({ revision: 3, values: { priceMin: 1800 } });
  });

  it("passes on the server's message and field problems", async () => {
    answer(400, JSON.stringify({ detail: "Fix the marked fields.", fields: { priceMin: "Use a number." } }));

    expect(await adminRequest("/hotels/7", "PUT", {})).toEqual({
      ok: false,
      failure: { status: 400, message: "Fix the marked fields.", fields: { priceMin: "Use a number." } },
    });
  });

  it("explains an out-of-date screen even when the answer isn't JSON", async () => {
    answer(409, "<html>Conflict</html>");

    const result = await adminRequest("/hotels/7", "PUT", {});

    expect(result).toMatchObject({ ok: false, failure: { status: 409, fields: {} } });
    expect(!result.ok && result.failure.message).toMatch(/Reload/);
  });

  it("ignores field problems that aren't text", async () => {
    answer(400, JSON.stringify({ detail: "Bad", fields: { priceMin: 5 } }));

    expect(await adminRequest("/x")).toMatchObject({ ok: false, failure: { fields: {} } });
  });

  it("reports when the network is down", async () => {
    vi.stubGlobal("fetch", vi.fn().mockRejectedValue(new TypeError("Failed to fetch")));

    expect(await adminRequest("/me")).toMatchObject({ ok: false, failure: { status: 0 } });
  });
});
