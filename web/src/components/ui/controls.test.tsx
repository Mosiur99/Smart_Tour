import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { useState } from "react";
import { describe, expect, it, vi } from "vitest";
import { expectAccessible } from "@/test/expectAccessible";
import { Button } from "./Button";
import { ChoiceGroup } from "./ChoiceGroup";
import { Stepper } from "./Stepper";
import { TextField } from "./TextField";

describe("Button", () => {
  it("does not submit forms unless asked", () => {
    render(<Button>Share</Button>);
    expect(screen.getByRole("button")).toHaveAttribute("type", "button");
  });

  it("blocks clicks while busy", async () => {
    const onClick = vi.fn();
    render(
      <Button busy onClick={onClick}>
        Plan
      </Button>,
    );
    await userEvent.click(screen.getByRole("button"));
    expect(onClick).not.toHaveBeenCalled();
    expect(screen.getByRole("button")).toHaveAttribute("aria-busy", "true");
  });
});

describe("TextField", () => {
  it("links the label and hint to the input", () => {
    render(<TextField label="Name" hint="As on your ticket" />);
    expect(screen.getByLabelText("Name")).toHaveAccessibleDescription("As on your ticket");
  });

  it("shows the error instead of the hint and marks the input invalid", () => {
    render(<TextField label="Name" hint="As on your ticket" error="Enter a name" />);
    const input = screen.getByLabelText("Name");
    expect(input).toHaveAccessibleDescription("Enter a name");
    expect(input).toBeInvalid();
    expect(screen.queryByText("As on your ticket")).not.toBeInTheDocument();
  });

  it("has no accessibility problems", async () => {
    const { container } = render(<TextField label="Name" error="Enter a name" />);
    await expectAccessible(container);
  });
});

function TripStyle() {
  const [style, setStyle] = useState("standard");
  return (
    <ChoiceGroup
      label="Trip style"
      value={style}
      onChange={setStyle}
      choices={[
        { value: "budget", label: "Budget" },
        { value: "standard", label: "Standard" },
      ]}
    />
  );
}

describe("ChoiceGroup", () => {
  it("changes the selection on click", async () => {
    render(<TripStyle />);
    await userEvent.click(screen.getByLabelText("Budget"));
    expect(screen.getByLabelText("Budget")).toBeChecked();
    expect(screen.getByLabelText("Standard")).not.toBeChecked();
  });

  it("has no accessibility problems", async () => {
    const { container } = render(<TripStyle />);
    await expectAccessible(container);
  });
});

function Days() {
  const [days, setDays] = useState(1);
  return (
    <Stepper
      label="Days"
      value={days}
      valueText={`${days} days`}
      min={1}
      max={2}
      onChange={setDays}
      decreaseLabel="Fewer days"
      increaseLabel="More days"
    />
  );
}

describe("Stepper", () => {
  it("stays inside its limits", async () => {
    render(<Days />);
    const fewer = screen.getByRole("button", { name: "Fewer days" });
    const more = screen.getByRole("button", { name: "More days" });

    expect(fewer).toBeDisabled();
    await userEvent.click(more);
    expect(screen.getByRole("status")).toHaveTextContent("2 days");
    expect(more).toBeDisabled();
    expect(fewer).toBeEnabled();
  });

  it("has no accessibility problems", async () => {
    const { container } = render(<Days />);
    await expectAccessible(container);
  });
});
