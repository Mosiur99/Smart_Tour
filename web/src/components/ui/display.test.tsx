import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { expectAccessible } from "@/test/expectAccessible";
import { Notice } from "./Notice";
import { RouteLine } from "./RouteLine";
import { Skeleton } from "./Skeleton";
import { TrustBadge } from "./TrustBadge";

describe("Notice", () => {
  it("interrupts screen readers only for problems", () => {
    render(
      <>
        <Notice tone="problem">Could not save</Notice>
        <Notice tone="caution">Prices are old</Notice>
      </>,
    );
    expect(screen.getByRole("alert")).toHaveTextContent("Could not save");
    expect(screen.getByRole("status")).toHaveTextContent("Prices are old");
  });
});

describe("TrustBadge", () => {
  it("states the trust level in words, not only colour", () => {
    render(<TrustBadge trust="verified">Verified Oct 2026</TrustBadge>);
    expect(screen.getByText("Verified Oct 2026")).toBeInTheDocument();
  });
});

describe("Skeleton", () => {
  it("is hidden from screen readers unless it has a label", () => {
    const { container } = render(<Skeleton />);
    expect(container.firstChild).toHaveAttribute("aria-hidden", "true");
    render(<Skeleton label="Loading plan" />);
    expect(screen.getByRole("status", { name: "Loading plan" })).toBeInTheDocument();
  });
});

describe("RouteLine", () => {
  const stops = [{ name: "Dhaka", legToNext: "Train" }, { name: "Sreemangal" }];

  it("keeps the stops in order as a list", () => {
    render(<RouteLine stops={stops} />);
    const items = screen.getAllByRole("listitem");
    expect(items[0]).toHaveTextContent("Dhaka");
    expect(items[1]).toHaveTextContent("Sreemangal");
  });

  it("draws no leg after the last stop", () => {
    render(<RouteLine stops={[{ name: "Dhaka", legToNext: "Train" }]} />);
    expect(screen.queryByText("Train")).not.toBeInTheDocument();
  });

  it("has no accessibility problems", async () => {
    const { container } = render(<RouteLine stops={stops} />);
    await expectAccessible(container);
  });
});
