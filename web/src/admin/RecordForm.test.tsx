import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { adminContext, HOTEL_SPEC, hotelRecord, LEG_SPEC } from "@/test/fixtures/admin";
import { expectAccessible } from "@/test/expectAccessible";
import { RecordForm } from "./RecordForm";

const HUBS = adminContext().hubs;

function renderHotelForm(overrides: Partial<Parameters<typeof RecordForm>[0]> = {}) {
  const onSubmit = vi.fn();
  const view = render(
    <RecordForm
      fields={HOTEL_SPEC.fields}
      values={hotelRecord().values}
      hubs={HUBS}
      creating={false}
      busy={false}
      errors={{}}
      submitLabel="Save changes"
      onSubmit={onSubmit}
      onCancel={vi.fn()}
      {...overrides}
    />,
  );
  return { ...view, onSubmit };
}

describe("RecordForm", () => {
  it("builds a labelled input for each field", async () => {
    const { container } = renderHotelForm();

    expect(screen.getByLabelText("Name in English (required)")).toHaveValue("Test Inn");
    expect(screen.getByLabelText("Name in Bangla")).toHaveAttribute("lang", "bn");
    expect(screen.getByLabelText("Tier")).toHaveValue("MID");
    expect(screen.getByLabelText("Lowest price a night (৳)")).toHaveValue(1500);
    expect(screen.getByLabelText("Checked on")).toHaveValue("2026-10-01");
    await expectAccessible(container);
  });

  it("sends typed values when submitted", async () => {
    const { onSubmit } = renderHotelForm();
    const price = screen.getByLabelText("Lowest price a night (৳)");

    await userEvent.clear(price);
    await userEvent.type(price, "1800");
    await userEvent.selectOptions(screen.getByLabelText("Tier"), "PREMIUM");
    await userEvent.click(screen.getByRole("button", { name: "Save changes" }));

    expect(onSubmit).toHaveBeenCalledWith(expect.objectContaining({ priceMin: 1800, tier: "PREMIUM", note: null }));
  });

  it("fills in today's date with one tap", async () => {
    const { onSubmit } = renderHotelForm();

    await userEvent.click(screen.getByRole("button", { name: "Checked today" }));
    await userEvent.click(screen.getByRole("button", { name: "Save changes" }));

    const today = new Date();
    const iso = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, "0")}-${String(today.getDate()).padStart(2, "0")}`;
    expect(onSubmit.mock.calls[0]?.[0].checkedAt).toBe(iso);
  });

  it("marks each field the server objected to", () => {
    renderHotelForm({ errors: { priceMin: "Use a number from 1 to 1000000." } });

    const price = screen.getByLabelText("Lowest price a night (৳)");
    expect(price).toHaveAttribute("aria-invalid", "true");
    expect(price).toHaveAccessibleDescription("Use a number from 1 to 1000000.");
  });

  it("only shows the record when it can't be changed", () => {
    renderHotelForm({ readOnly: true });

    expect(screen.queryByRole("button", { name: "Save changes" })).not.toBeInTheDocument();
    expect(screen.getByLabelText("Name in English (required)")).toBeDisabled();
    expect(screen.getByRole("button", { name: "Close" })).toBeEnabled();
  });

  it("ticks months and keeps a journey's ends fixed once it exists", async () => {
    const onSubmit = vi.fn();
    render(
      <RecordForm
        fields={LEG_SPEC.fields}
        values={{ fromHub: "dhaka", toHub: "sylhet", serviceName: "Green Line", months: [1, 2] }}
        hubs={HUBS}
        creating={false}
        busy={false}
        errors={{}}
        submitLabel="Save changes"
        onSubmit={onSubmit}
        onCancel={vi.fn()}
      />,
    );

    expect(screen.getByLabelText("From (required)")).toBeDisabled();
    await userEvent.click(screen.getByRole("checkbox", { name: "Jan" }));
    await userEvent.click(screen.getByRole("checkbox", { name: "Dec" }));
    await userEvent.click(screen.getByRole("button", { name: "Save changes" }));

    expect(onSubmit).toHaveBeenCalledWith(expect.objectContaining({ months: [2, 12], fromHub: "dhaka" }));
  });
});
