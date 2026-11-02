"use client";

import { useRouter, useSearchParams } from "next/navigation";
import { useEffect, useMemo, useState, type FormEvent } from "react";
import { createPlan, fetchHubs, searchDestinations, type PlanFailure } from "@/api/browser";
import type { BudgetLevel, DestinationSummary, Hub, Interest } from "@/api/types";
import { Button } from "@/components/ui/Button";
import { ChoiceGroup } from "@/components/ui/ChoiceGroup";
import { Notice } from "@/components/ui/Notice";
import { PlaceSearch, type PlaceOption } from "@/components/ui/PlaceSearch";
import { Stepper } from "@/components/ui/Stepper";
import { localePath, type Locale } from "@/i18n/config";
import type { Dictionary } from "@/i18n/dictionary";
import { formatCount, monthNames } from "@/i18n/format";
import { matchHubs, toOption } from "./placeOptions";

const BUDGETS: BudgetLevel[] = ["BUDGET", "MID", "PREMIUM"];
const INTERESTS: Interest[] = ["NATURE", "HERITAGE", "ADVENTURE", "RELAXATION", "CULTURE"];
const SEARCH_DELAY_MS = 200;

/** Only the plain-text sections, so the browser never downloads the whole dictionary. */
export type PlanFormText = Pick<Dictionary, "form" | "count">;

export function PlanForm({ locale, text }: { locale: Locale; text: PlanFormText }) {
  const router = useRouter();
  const prefilledDestination = useSearchParams().get("to");

  const [hubs, setHubs] = useState<Hub[]>([]);
  const [fromQuery, setFromQuery] = useState("");
  const [from, setFrom] = useState<PlaceOption | null>(null);
  const [toQuery, setToQuery] = useState("");
  const [to, setTo] = useState<PlaceOption | null>(null);
  const [destinations, setDestinations] = useState<DestinationSummary[]>([]);
  const [days, setDays] = useState(3);
  const [people, setPeople] = useState(2);
  const [budget, setBudget] = useState<BudgetLevel>("MID");
  const [month, setMonth] = useState(1);
  const [interests, setInterests] = useState<Interest[]>([]);
  const [showErrors, setShowErrors] = useState(false);
  const [failure, setFailure] = useState<PlanFailure | null>(null);
  const [busy, setBusy] = useState(false);

  // Read in the browser: the page is built once, so the server can't know the month.
  useEffect(() => setMonth(new Date().getMonth() + 1), []);

  useEffect(() => {
    fetchHubs().then(setHubs, () => setHubs([]));
  }, []);

  // A destination page links here with ?to=<slug> to start a plan for that place.
  useEffect(() => {
    if (!prefilledDestination) {
      return;
    }
    const controller = new AbortController();
    searchDestinations(prefilledDestination, controller.signal).then((found) => {
      const match = found.find((place) => place.slug === prefilledDestination);
      if (match) {
        const option = toOption(match, locale);
        setTo(option);
        setToQuery(option.name);
      }
    }, ignoreAbort);
    return () => controller.abort();
  }, [prefilledDestination, locale]);

  useEffect(() => {
    const query = toQuery.trim();
    if (!query || query === to?.name) {
      setDestinations([]);
      return;
    }
    const controller = new AbortController();
    const timer = setTimeout(() => {
      searchDestinations(query, controller.signal).then(setDestinations, ignoreAbort);
    }, SEARCH_DELAY_MS);
    return () => {
      clearTimeout(timer);
      controller.abort();
    };
  }, [toQuery, to]);

  const fromOptions = useMemo(
    () => matchHubs(hubs, fromQuery).map((hub) => toOption(hub, locale)),
    [hubs, fromQuery, locale],
  );
  const toOptions = useMemo(
    () => destinations.map((place) => toOption(place, locale)),
    [destinations, locale],
  );

  function toggleInterest(interest: Interest) {
    setInterests((current) =>
      current.includes(interest) ? current.filter((item) => item !== interest) : [...current, interest],
    );
  }

  async function submit(event: FormEvent) {
    event.preventDefault();
    setShowErrors(true);
    setFailure(null);
    if (!from || !to) {
      return;
    }
    setBusy(true);
    const result = await createPlan({
      originHub: from.id,
      destination: to.id,
      days,
      people,
      budget,
      month,
      interests,
    });
    if (result.ok) {
      router.push(localePath(locale, `/plan/${result.publicId}`));
      return;
    }
    setFailure(result.reason);
    setBusy(false);
  }

  return (
    <form onSubmit={submit} noValidate className="flex flex-col gap-5">
      <PlaceSearch
        label={text.form.from}
        query={fromQuery}
        onQueryChange={(query) => {
          setFromQuery(query);
          setFrom(null);
        }}
        options={fromOptions}
        onSelect={(option) => {
          setFrom(option);
          setFromQuery(option.name);
        }}
        noResultsText={text.form.noPlace}
        error={showErrors && !from ? text.form.pickFrom : undefined}
      />
      <PlaceSearch
        label={text.form.to}
        query={toQuery}
        onQueryChange={(query) => {
          setToQuery(query);
          setTo(null);
        }}
        options={toOptions}
        onSelect={(option) => {
          setTo(option);
          setToQuery(option.name);
        }}
        noResultsText={text.form.noPlace}
        error={showErrors && !to ? text.form.pickTo : undefined}
      />

      <div className="grid grid-cols-1 gap-5 sm:grid-cols-2">
        <Stepper
          label={text.form.days}
          value={days}
          valueText={formatCount(text.count.days, days, locale)}
          min={1}
          max={14}
          onChange={setDays}
          decreaseLabel={text.form.fewerDays}
          increaseLabel={text.form.moreDays}
        />
        <Stepper
          label={text.form.people}
          value={people}
          valueText={formatCount(text.count.people, people, locale)}
          min={1}
          max={30}
          onChange={setPeople}
          decreaseLabel={text.form.fewerPeople}
          increaseLabel={text.form.morePeople}
        />
      </div>

      <ChoiceGroup
        label={text.form.budget}
        value={budget}
        onChange={setBudget}
        choices={BUDGETS.map((value) => ({ value, label: text.form.budgets[value] }))}
      />

      <div className="flex flex-col gap-1">
        <label htmlFor="month" className="font-semibold">
          {text.form.month}
        </label>
        <select
          id="month"
          value={month}
          onChange={(event) => setMonth(Number(event.target.value))}
          className="min-h-12 rounded-control border border-stitch bg-surface px-3 text-ink"
        >
          {monthNames(locale).map((name, index) => (
            <option key={name} value={index + 1}>
              {name}
            </option>
          ))}
        </select>
      </div>

      <fieldset className="m-0 min-w-0 border-0 p-0">
        <legend className="mb-2 p-0 font-semibold">{text.form.interests}</legend>
        <div className="flex flex-wrap gap-2">
          {INTERESTS.map((interest) => (
            <label key={interest} className="cursor-pointer">
              <input
                type="checkbox"
                checked={interests.includes(interest)}
                onChange={() => toggleInterest(interest)}
                className="peer sr-only"
              />
              <span className="flex min-h-11 items-center rounded-full border border-stitch px-4 text-muted peer-checked:border-accent peer-checked:bg-soft peer-checked:font-semibold peer-checked:text-soft-ink peer-focus-visible:outline-2 peer-focus-visible:outline-offset-2 peer-focus-visible:outline-accent">
                {text.form.interestNames[interest]}
              </span>
            </label>
          ))}
        </div>
      </fieldset>

      {failure && <Notice tone="problem">{text.form.errors[failure]}</Notice>}

      <Button type="submit" busy={busy} className="min-h-13 text-lg">
        {text.form.submit}
      </Button>
    </form>
  );
}

function ignoreAbort(error: unknown) {
  if (!(error instanceof DOMException && error.name === "AbortError")) {
    throw error;
  }
}
