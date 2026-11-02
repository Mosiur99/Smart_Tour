import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import type { AdminRecord } from "@/api/types";
import { adminContext, hotelRecord } from "@/test/fixtures/admin";
import { AdminProvider } from "./AdminShell";
import { RecordCard } from "./RecordCard";

function renderCard(record: AdminRecord, role: "EDITOR" | "PUBLISHER" = "PUBLISHER") {
  const onChanged = vi.fn();
  render(
    <AdminProvider value={adminContext(role)}>
      <RecordCard record={record} onChanged={onChanged} />
    </AdminProvider>,
  );
  return onChanged;
}

function answer(status: number, body: unknown) {
  const fetchMock = vi.fn().mockResolvedValue(new Response(JSON.stringify(body), { status }));
  vi.stubGlobal("fetch", fetchMock);
  return fetchMock;
}

function sentBody(fetchMock: ReturnType<typeof vi.fn>) {
  const [url, init] = fetchMock.mock.calls[0] as [string, RequestInit];
  return { url, method: init.method, body: JSON.parse(init.body as string) as unknown };
}

describe("RecordCard", () => {
  it("shows the status and when it was checked", () => {
    renderCard(hotelRecord());

    expect(screen.getByRole("heading", { name: "Test Inn" })).toBeInTheDocument();
    expect(screen.getByText("Published")).toBeInTheDocument();
    expect(screen.getByText(/Checked 1 Oct 2026/)).toBeInTheDocument();
  });

  it("saves only the fields that changed, with the revision it was opened at", async () => {
    const saved = hotelRecord({ revision: 4, values: { ...hotelRecord().values, priceMin: 1800 } });
    const fetchMock = answer(200, saved);
    const onChanged = renderCard(hotelRecord());

    await userEvent.click(screen.getByRole("button", { name: "Edit" }));
    const price = screen.getByLabelText("Lowest price a night (৳)");
    await userEvent.clear(price);
    await userEvent.type(price, "1800");
    await userEvent.click(screen.getByRole("button", { name: "Save changes" }));

    expect(sentBody(fetchMock)).toEqual({
      url: "/api/v1/admin/hotels/7",
      method: "PUT",
      body: { revision: 3, values: { priceMin: 1800 } },
    });
    expect(onChanged).toHaveBeenCalledWith(saved);
    expect(await screen.findByText("Saved.")).toBeInTheDocument();
  });

  it("doesn't call the server when nothing changed", async () => {
    const fetchMock = answer(200, {});
    renderCard(hotelRecord());

    await userEvent.click(screen.getByRole("button", { name: "Edit" }));
    await userEvent.click(screen.getByRole("button", { name: "Save changes" }));

    expect(fetchMock).not.toHaveBeenCalled();
  });

  it("offers a reload when someone else saved first", async () => {
    answer(409, { detail: "Someone saved this record after you opened it." });
    renderCard(hotelRecord());

    await userEvent.click(screen.getByRole("button", { name: "Edit" }));
    await userEvent.type(screen.getByLabelText("Name in English (required)"), "!");
    await userEvent.click(screen.getByRole("button", { name: "Save changes" }));

    expect(await screen.findByText("Someone saved this record after you opened it.")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Reload" })).toBeInTheDocument();
  });

  it("publishes a draft", async () => {
    const fetchMock = answer(200, hotelRecord({ status: "PUBLISHED" }));
    renderCard(hotelRecord({ status: "DRAFT" }));

    await userEvent.click(screen.getByRole("button", { name: "Publish" }));

    expect(sentBody(fetchMock)).toEqual({
      url: "/api/v1/admin/hotels/7/status",
      method: "POST",
      body: { revision: 3, status: "PUBLISHED" },
    });
    expect(await screen.findByText("Published.")).toBeInTheDocument();
  });

  it("keeps taking a record off the site behind one more tap", async () => {
    const fetchMock = answer(200, hotelRecord({ status: "DRAFT" }));
    renderCard(hotelRecord());

    expect(screen.queryByRole("button", { name: "Publish" })).not.toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Unpublish" })).not.toBeVisible();
    await userEvent.click(screen.getByText("More"));
    await userEvent.click(screen.getByRole("button", { name: "Unpublish" }));

    expect(sentBody(fetchMock).body).toEqual({ revision: 3, status: "DRAFT" });
    expect(await screen.findByText("Unpublished. It's a draft again.")).toBeInTheDocument();
  });

  it("explains why a record can't be published yet", async () => {
    answer(422, { detail: "Add the tier, map position, prices, source and checked date before publishing." });
    renderCard(hotelRecord({ status: "DRAFT" }));

    await userEvent.click(screen.getByRole("button", { name: "Publish" }));

    expect(await screen.findByRole("alert")).toHaveTextContent("Add the tier");
  });

  it("lets an editor only look at what visitors see", async () => {
    renderCard(hotelRecord({ status: "IMPORTED" }), "EDITOR");

    expect(screen.queryByRole("button", { name: "Publish" })).not.toBeInTheDocument();
    await userEvent.click(screen.getByRole("button", { name: "View" }));
    expect(screen.getByText("Only a publisher can change what visitors see.")).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Save changes" })).not.toBeInTheDocument();
  });

  it("lets an editor work on drafts", async () => {
    renderCard(hotelRecord({ status: "DRAFT" }), "EDITOR");

    await userEvent.click(screen.getByRole("button", { name: "Edit" }));

    expect(screen.getByRole("button", { name: "Save changes" })).toBeInTheDocument();
  });
});
