import { render, screen, within } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import type { PlanResponse, TripPlan } from "@/api/types";
import saved from "@/test/fixtures/plan.json";
import { expectAccessible } from "@/test/expectAccessible";
import { dictionary } from "@/i18n/dictionary";
import type { Locale } from "@/i18n/config";
import { legSummary, missingLabel, PlanView } from "./PlanView";

// A real response from the API with e2e/test-data.sql loaded.
const PLAN = (saved as unknown as PlanResponse).plan as TripPlan;
const HUBS: Record<string, string> = { dhaka: "Dhaka", sylhet: "Sylhet" };

function show(locale: Locale, plan: TripPlan = PLAN) {
  const text = dictionary(locale);
  return render(
    <PlanView
      plan={plan}
      fromName="Dhaka"
      budgetName={text.form.budgets.MID}
      hubName={(slug) => HUBS[slug ?? ""] ?? ""}
      locale={locale}
      text={text}
    />,
  );
}

describe("PlanView", () => {
  it("shows the title, total and every day", () => {
    show("en");
    expect(screen.getByRole("heading", { level: 1 })).toHaveTextContent("3 days in Sylhet");
    expect(screen.getByText("৳11,060 to ৳14,600")).toBeInTheDocument();
    for (const day of ["Day 1", "Day 2", "Day 3"]) {
      expect(screen.getByRole("heading", { name: day })).toBeInTheDocument();
    }
  });

  it("lists every cost line with its range", () => {
    show("en");
    const budget = screen.getByRole("region", { name: "Estimated total" });
    expect(within(budget).getByText("Hotel").nextSibling).toHaveTextContent("৳4,000 to ৳6,000");
    expect(within(budget).getByText("Food").nextSibling).toHaveTextContent("৳3,600");
  });

  it("names what the total leaves out, in the visitor's language", () => {
    show("bn");
    expect(screen.getByText("পরীক্ষা মাজার প্রবেশ")).toBeInTheDocument();
  });

  it("writes every number in Bangla digits", () => {
    show("bn");
    expect(screen.getByRole("heading", { level: 1 })).toHaveTextContent("৩ দিনে সিলেট");
    expect(screen.getByText("৳১১,০৬০ থেকে ৳১৪,৬০০")).toBeInTheDocument();
  });

  it("shows notices and leaves out the hotel when there is none", () => {
    show("en", { ...PLAN, hotel: null, notices: ["NO_HOTEL_AVAILABLE"] });
    expect(screen.getByText(/We have no checked hotel here yet/)).toHaveAttribute("role", "status");
    expect(screen.queryByRole("heading", { name: "Where to stay" })).not.toBeInTheDocument();
  });

  it("has no accessibility problems", async () => {
    const { container } = show("bn");
    await expectAccessible(container);
  });
});

describe("missingLabel", () => {
  const text = dictionary("en");

  it("names the place for an entry fee", () => {
    expect(missingLabel({ category: "ENTRY_FEES", nameEn: "Lalakhal", nameBn: "লালাখাল" }, "en", text)).toBe(
      "Entry to Lalakhal",
    );
  });

  it("uses the category when there is no name", () => {
    expect(missingLabel({ category: "FOOD", nameEn: null, nameBn: null }, "en", text)).toBe("Food");
  });
});

describe("legSummary", () => {
  const leg = PLAN.outbound.legs[0];

  it("joins mode, time and fare", () => {
    expect(legSummary(leg, "en", dictionary("en"))).toBe("Bus · 7 h · ৳540 to ৳900");
  });

  it("says when the fare is unknown", () => {
    expect(legSummary({ ...leg, fareMin: null, fareMax: null }, "en", dictionary("en"))).toBe(
      "Bus · 7 h · fare unknown",
    );
  });
});
