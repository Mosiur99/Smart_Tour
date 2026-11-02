import { revalidateTag } from "next/cache";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { POST } from "./route";

vi.mock("next/cache", () => ({ revalidateTag: vi.fn() }));
vi.mock("server-only", () => ({}));

function call(secret?: string) {
  const headers: Record<string, string> = secret === undefined ? {} : { "x-refresh-secret": secret };
  return POST(new Request("http://localhost/hooks/catalog-changed", { method: "POST", headers }));
}

describe("POST /hooks/catalog-changed", () => {
  beforeEach(() => {
    vi.mocked(revalidateTag).mockClear();
    vi.stubEnv("REFRESH_SECRET", "shared-secret");
  });

  it("refreshes every catalog page when the secret matches", async () => {
    const response = await call("shared-secret");

    expect(response.status).toBe(200);
    expect(revalidateTag).toHaveBeenCalledWith("catalog", { expire: 0 });
  });

  it.each([undefined, "wrong-secret", "shared-secre", ""])("hides itself from a caller with secret %s", async (secret) => {
    const response = await call(secret);

    expect(response.status).toBe(404);
    expect(revalidateTag).not.toHaveBeenCalled();
  });

  it("stays off when no secret is set", async () => {
    vi.stubEnv("REFRESH_SECRET", "");

    expect((await call("")).status).toBe(404);
    expect(revalidateTag).not.toHaveBeenCalled();
  });
});
