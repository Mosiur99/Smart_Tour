import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { useState } from "react";
import { describe, expect, it, vi } from "vitest";
import { expectAccessible } from "@/test/expectAccessible";
import { PlaceSearch, type PlaceOption } from "./PlaceSearch";

const PLACES: PlaceOption[] = [
  { id: "sreemangal", name: "Sreemangal", otherName: "শ্রীমঙ্গল" },
  { id: "sreepur", name: "Sreepur" },
];

function Search({
  onSelect,
  options = PLACES,
}: {
  onSelect?: (option: PlaceOption) => void;
  options?: PlaceOption[];
}) {
  const [query, setQuery] = useState("");
  return (
    <PlaceSearch
      label="Where to?"
      query={query}
      onQueryChange={setQuery}
      options={options}
      onSelect={(option) => {
        setQuery(option.name);
        onSelect?.(option);
      }}
      noResultsText="No place found"
    />
  );
}

describe("PlaceSearch", () => {
  it("keeps the list closed until the visitor types", () => {
    render(<Search />);
    expect(screen.getByRole("combobox")).toHaveAttribute("aria-expanded", "false");
    expect(screen.queryByRole("listbox")).not.toBeInTheDocument();
  });

  it("selects with the keyboard", async () => {
    const onSelect = vi.fn();
    render(<Search onSelect={onSelect} />);

    await userEvent.type(screen.getByRole("combobox"), "sree{ArrowDown}{Enter}");

    expect(onSelect).toHaveBeenCalledWith(PLACES[1]);
    expect(screen.getByRole("combobox")).toHaveValue("Sreepur");
    expect(screen.getByRole("combobox")).toHaveAttribute("aria-expanded", "false");
  });

  it("wraps around when moving past either end", async () => {
    render(<Search />);
    await userEvent.type(screen.getByRole("combobox"), "sree{ArrowUp}");
    expect(screen.getByRole("option", { name: /Sreepur/ })).toHaveAttribute(
      "aria-selected",
      "true",
    );
  });

  it("points assistive technology at the active option", async () => {
    render(<Search />);
    const input = screen.getByRole("combobox");
    await userEvent.type(input, "sree");
    expect(input.getAttribute("aria-activedescendant")).toBe(
      screen.getByRole("option", { name: /Sreemangal/ }).id,
    );
  });

  it("selects with the mouse", async () => {
    const onSelect = vi.fn();
    render(<Search onSelect={onSelect} />);
    await userEvent.type(screen.getByRole("combobox"), "sree");
    await userEvent.click(screen.getByRole("option", { name: /Sreemangal/ }));
    expect(onSelect).toHaveBeenCalledWith(PLACES[0]);
  });

  it("closes on Escape without selecting", async () => {
    const onSelect = vi.fn();
    render(<Search onSelect={onSelect} />);
    await userEvent.type(screen.getByRole("combobox"), "sree{Escape}");
    expect(screen.queryByRole("listbox")).not.toBeInTheDocument();
    expect(onSelect).not.toHaveBeenCalled();
  });

  it("says so when nothing matches, and Enter selects nothing", async () => {
    const onSelect = vi.fn();
    render(<Search onSelect={onSelect} options={[]} />);
    await userEvent.type(screen.getByRole("combobox"), "zzz{Enter}");
    expect(onSelect).not.toHaveBeenCalled();
    expect(screen.getByText("No place found")).toBeInTheDocument();
  });

  it("has no accessibility problems while open", async () => {
    const { container } = render(<Search />);
    await userEvent.type(screen.getByRole("combobox"), "sree");
    await expectAccessible(container);
  });
});
