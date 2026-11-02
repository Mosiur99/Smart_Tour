import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { ThemeToggle } from "./ThemeToggle";

function deviceTheme(dark: boolean) {
  vi.stubGlobal("matchMedia", (query: string) => ({ matches: dark, media: query }));
}

const toggle = () => render(<ThemeToggle toDarkLabel="Dark" toLightLabel="Light" />);

describe("ThemeToggle", () => {
  beforeEach(() => {
    delete document.documentElement.dataset.theme;
    localStorage.clear();
  });

  it("starts from the device setting", () => {
    deviceTheme(true);
    toggle();
    expect(screen.getByRole("button")).toHaveTextContent("Light");
  });

  it("switches the page and remembers the choice", async () => {
    deviceTheme(false);
    toggle();
    await userEvent.click(screen.getByRole("button"));
    expect(document.documentElement.dataset.theme).toBe("dark");
    expect(localStorage.getItem("theme")).toBe("dark");
  });

  it("still switches when storage is blocked", async () => {
    deviceTheme(false);
    vi.spyOn(Storage.prototype, "setItem").mockImplementation(() => {
      throw new Error("blocked");
    });
    toggle();
    await userEvent.click(screen.getByRole("button"));
    expect(document.documentElement.dataset.theme).toBe("dark");
  });
});
