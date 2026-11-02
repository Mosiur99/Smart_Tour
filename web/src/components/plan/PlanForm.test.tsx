import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { dictionary } from "@/i18n/dictionary";
import type { Locale } from "@/i18n/config";
import { PlanForm as Form } from "./PlanForm";

function PlanForm({ locale }: { locale: Locale }) {
  const text = dictionary(locale);
  return <Form locale={locale} text={{ form: text.form, count: text.count }} />;
}

const push = vi.fn();
let searchParams = new URLSearchParams();

vi.mock("next/navigation", () => ({
  useRouter: () => ({ push }),
  useSearchParams: () => searchParams,
}));

const HUBS = [
  { slug: "dhaka", nameEn: "Dhaka", nameBn: "ঢাকা" },
  { slug: "sylhet", nameEn: "Sylhet", nameBn: "সিলেট" },
];
const SYLHET = {
  slug: "sylhet",
  nameEn: "Sylhet",
  nameBn: "সিলেট",
  divisionSlug: "sylhet",
  divisionNameEn: "Sylhet",
  divisionNameBn: "সিলেট",
  verified: true,
};

/** A fake API: hubs, search, and a plan answer chosen per test. */
function fakeApi(planStatus = 201) {
  const fetchMock = vi.fn(async (url: string, init?: RequestInit) => {
    if (url === "/api/v1/hubs") {
      return Response.json(HUBS);
    }
    if (url.startsWith("/api/v1/destinations")) {
      return Response.json({ items: [SYLHET], nextCursor: null });
    }
    if (url === "/api/v1/plans" && init?.method === "POST") {
      return Response.json({ publicId: "7W7TmZWry2a6" }, { status: planStatus });
    }
    return new Response(null, { status: 404 });
  });
  vi.stubGlobal("fetch", fetchMock);
  return fetchMock;
}

async function fillPlaces() {
  await userEvent.type(screen.getByRole("combobox", { name: "Starting from" }), "dha");
  await userEvent.click(await screen.findByRole("option", { name: /Dhaka/ }));
  await userEvent.type(screen.getByRole("combobox", { name: "Going to" }), "syl");
  await userEvent.click(await screen.findByRole("option", { name: /Sylhet/ }));
}

describe("PlanForm", () => {
  beforeEach(() => {
    push.mockReset();
    searchParams = new URLSearchParams();
  });

  it("sends the trip and opens the plan", async () => {
    const fetchMock = fakeApi();
    render(<PlanForm locale="en" />);
    await fillPlaces();
    await userEvent.click(screen.getByRole("button", { name: "More people" }));
    await userEvent.click(screen.getByLabelText("Nature"));
    await userEvent.click(screen.getByRole("button", { name: "Make my plan" }));

    const [, init] = fetchMock.mock.calls.find(([url]) => url === "/api/v1/plans")!;
    expect(JSON.parse(init!.body as string)).toMatchObject({
      originHub: "dhaka",
      destination: "sylhet",
      days: 3,
      people: 3,
      budget: "MID",
      interests: ["NATURE"],
    });
    expect(push).toHaveBeenCalledWith("/en/plan/7W7TmZWry2a6");
  });

  it("opens the Bangla plan address from the Bangla form", async () => {
    fakeApi();
    render(<PlanForm locale="bn" />);
    await userEvent.type(screen.getByRole("combobox", { name: "কোথা থেকে" }), "ঢাকা");
    await userEvent.click(await screen.findByRole("option", { name: /ঢাকা/ }));
    await userEvent.type(screen.getByRole("combobox", { name: "কোথায়" }), "সিলে");
    await userEvent.click(await screen.findByRole("option", { name: /সিলেট/ }));
    await userEvent.click(screen.getByRole("button", { name: "প্ল্যান তৈরি করুন" }));
    expect(push).toHaveBeenCalledWith("/plan/7W7TmZWry2a6");
  });

  it("asks for both places and sends nothing until then", async () => {
    const fetchMock = fakeApi();
    render(<PlanForm locale="en" />);
    await userEvent.click(screen.getByRole("button", { name: "Make my plan" }));

    expect(screen.getByRole("combobox", { name: "Starting from" })).toHaveAccessibleDescription(
      "Choose a starting point from the list.",
    );
    expect(fetchMock.mock.calls.some(([url]) => url === "/api/v1/plans")).toBe(false);
  });

  it("forgets a chosen place when the visitor types over it", async () => {
    fakeApi();
    render(<PlanForm locale="en" />);
    await fillPlaces();
    await userEvent.type(screen.getByRole("combobox", { name: "Going to" }), "x");
    await userEvent.click(screen.getByRole("button", { name: "Make my plan" }));
    expect(screen.getByText("Choose a destination from the list.")).toBeInTheDocument();
    expect(push).not.toHaveBeenCalled();
  });

  it("explains a refusal and lets the visitor try again", async () => {
    fakeApi(429);
    render(<PlanForm locale="en" />);
    await fillPlaces();
    await userEvent.click(screen.getByRole("button", { name: "Make my plan" }));

    expect(await screen.findByRole("alert")).toHaveTextContent("Too many plans");
    expect(screen.getByRole("button", { name: "Make my plan" })).toBeEnabled();
  });

  it("fills in the destination from ?to=", async () => {
    fakeApi();
    searchParams = new URLSearchParams("to=sylhet");
    render(<PlanForm locale="en" />);
    expect(await screen.findByDisplayValue("Sylhet")).toBe(screen.getByRole("combobox", { name: "Going to" }));
  });
});
