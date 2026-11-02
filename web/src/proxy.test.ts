import { NextRequest } from "next/server";
import { describe, expect, it, vi } from "vitest";
import { proxy } from "./proxy";

function visit(path: string, host = "smarttour.example") {
  return proxy(new NextRequest(`https://${host}${path}`, { headers: { host } }));
}

describe("proxy", () => {
  it("serves Bangla pages at the root", () => {
    expect(visit("/destinations").headers.get("x-middleware-rewrite")).toBe("https://smarttour.example/bn/destinations");
  });

  it("sends a typed /bn address to the root", () => {
    const response = visit("/bn/destinations");
    expect(response.status).toBe(308);
    expect(response.headers.get("location")).toBe("https://smarttour.example/destinations");
  });

  it("opens the admin panel on its own address only", () => {
    vi.stubEnv("ADMIN_HOST", "admin.smarttour.example");

    expect(visit("/admin", "smarttour.example").status).toBe(404);
    expect(visit("/admin/history", "smarttour.example").status).toBe(404);
    expect(visit("/admin", "admin.smarttour.example").headers.get("x-middleware-next")).toBe("1");
  });

  it("opens the admin panel anywhere when no admin address is set", () => {
    vi.stubEnv("ADMIN_HOST", "");

    expect(visit("/admin").headers.get("x-middleware-next")).toBe("1");
  });
});
