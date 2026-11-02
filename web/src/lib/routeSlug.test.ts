import { describe, expect, it } from "vitest";
import { parseRouteSlug, routeSlug } from "./routeSlug";

describe("parseRouteSlug", () => {
  it("splits at the first -to-", () => {
    expect(parseRouteSlug("dhaka-to-coxs-bazar")).toEqual({ from: "dhaka", to: "coxs-bazar" });
  });

  it("reverses routeSlug", () => {
    expect(parseRouteSlug(routeSlug("sylhet", "dhaka"))).toEqual({ from: "sylhet", to: "dhaka" });
  });

  it.each(["dhaka", "dhaka-to-", "-to-sylhet", "Dhaka-to-sylhet", "dhaka-to-syl%20het"])(
    "rejects %s",
    (route) => {
      expect(parseRouteSlug(route)).toBeNull();
    },
  );
});
