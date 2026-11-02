import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { expectAccessible } from "@/test/expectAccessible";
import { DesignPreview } from "./DesignPreview";

describe("DesignPreview", () => {
  beforeEach(() => {
    vi.stubGlobal("matchMedia", () => ({ matches: false }));
  });

  it("switches the page to Bangla, including the lang attribute", async () => {
    render(<DesignPreview />);
    await userEvent.click(screen.getByRole("button", { name: "বাংলা" }));

    expect(screen.getByRole("heading", { level: 1 })).toHaveTextContent("তিন দিনে শ্রীমঙ্গল");
    expect(screen.getByRole("heading", { level: 1 }).closest("[lang]")).toHaveAttribute("lang", "bn");
    expect(screen.getByText("৳৮,৪০০ থেকে ৳১৩,৬০০")).toBeInTheDocument();
  });

  it("finds a place by its Bangla or English name", async () => {
    render(<DesignPreview />);
    await userEvent.type(screen.getByRole("combobox"), "ঢাকা");
    expect(screen.getByRole("option", { name: /Dhaka/ })).toBeInTheDocument();
  });

  it("has no accessibility problems", async () => {
    const { container } = render(<DesignPreview />);
    await expectAccessible(container);
  });
});
