import { render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { LanguageSwitch } from "./LanguageSwitch";

let pathname = "/";
vi.mock("next/navigation", () => ({ usePathname: () => pathname }));

describe("LanguageSwitch", () => {
  it("keeps the visitor on the same page in English", () => {
    pathname = "/plan/7W7TmZWry2a6";
    render(<LanguageSwitch locale="bn" text="English" label="View in English" />);
    expect(screen.getByRole("link", { name: "View in English" })).toHaveAttribute("href", "/en/plan/7W7TmZWry2a6");
  });

  it("keeps the visitor on the same page in Bangla", () => {
    pathname = "/en/destinations/sylhet";
    render(<LanguageSwitch locale="en" text="বাংলা" label="বাংলায় দেখুন" />);
    expect(screen.getByRole("link")).toHaveAttribute("href", "/destinations/sylhet");
    expect(screen.getByRole("link")).toHaveAttribute("lang", "bn");
  });
});
