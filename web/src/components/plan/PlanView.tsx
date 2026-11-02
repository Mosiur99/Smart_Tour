import Link from "next/link";
import type { DayPlan, Journey, Leg, MissingCost, Stop, TripPlan } from "@/api/types";
import { Notice } from "@/components/ui/Notice";
import { RouteLine, type RouteStop } from "@/components/ui/RouteLine";
import { localName, localePath, type Locale } from "@/i18n/config";
import type { Dictionary } from "@/i18n/dictionary";
import { formatCount, formatDuration, formatNumber } from "@/i18n/format";
import { formatTakaRange } from "@/lib/money";
import { ShareActions } from "./ShareActions";

type PlanViewProps = {
  plan: TripPlan;
  fromName: string;
  budgetName: string;
  hubName: (slug: string | null) => string;
  locale: Locale;
  text: Dictionary;
};

export function planTitle(plan: TripPlan, locale: Locale, text: Dictionary): string {
  const place = localName(locale, { nameEn: plan.destinationNameEn, nameBn: plan.destinationNameBn });
  return text.plan.title(formatCount(text.count.days, plan.days, locale), place);
}

export function PlanView({ plan, fromName, budgetName, hubName, locale, text }: PlanViewProps) {
  const title = planTitle(plan, locale, text);
  const people = formatCount(text.count.people, plan.people, locale);

  return (
    <article className="flex flex-col gap-8">
      <header className="flex flex-col gap-2">
        <p className="m-0 text-muted">{text.plan.summary(fromName, people, budgetName)}</p>
        <h1 className="m-0 text-[32px]">{title}</h1>
      </header>

      {plan.notices.length > 0 && (
        <div className="flex flex-col gap-2">
          {plan.notices.map((notice) => (
            <Notice key={notice} tone="caution">
              {text.plan.notices[notice]}
            </Notice>
          ))}
        </div>
      )}

      <Budget plan={plan} locale={locale} text={text} />

      {plan.outbound && (
        <JourneySection title={text.plan.gettingThere} journey={plan.outbound} {...{ hubName, locale, text }} />
      )}

      {plan.dayPlans.map((day) => (
        <Day key={day.day} day={day} locale={locale} text={text} />
      ))}

      {plan.hotel && (
        <section className="flex flex-col gap-2 rounded-card border border-line bg-surface p-5">
          <h2 className="m-0 text-xl">{text.plan.hotel}</h2>
          <p className="m-0 font-semibold">{localName(locale, plan.hotel)}</p>
          <p className="m-0 text-muted">
            {text.plan.perNight(formatTakaRange(plan.hotel.priceMin, plan.hotel.priceMax, locale))}
          </p>
          <p className="m-0 text-muted">
            {text.plan.rooms(
              formatCount(text.count.rooms, plan.rooms, locale),
              formatCount(text.count.nights, plan.nights, locale),
            )}
          </p>
        </section>
      )}

      {plan.inbound && (
        <JourneySection title={text.plan.goingHome} journey={plan.inbound} {...{ hubName, locale, text }} />
      )}

      <ShareActions title={title} shareText={text.plan.shareText(title)} text={text.share} />
      <Link href={localePath(locale, "/")} className="text-accent print:hidden">
        {text.plan.newPlan}
      </Link>
    </article>
  );
}

function Budget({ plan, locale, text }: { plan: TripPlan; locale: Locale; text: Dictionary }) {
  const { budget } = plan;
  return (
    <section
      aria-labelledby="budget-title"
      className="flex flex-col gap-3 rounded-card border border-line bg-surface p-5"
    >
      <div className="flex flex-col gap-1">
        <h2 id="budget-title" className="m-0 font-body text-base font-normal text-muted">
          {text.plan.total}
        </h2>
        <p className="m-0 font-display text-[28px] leading-tight font-semibold">
          {formatTakaRange(budget.low, budget.high, locale)}
        </p>
        <p className="m-0 text-sm text-muted">
          {text.plan.forPeople(formatCount(text.count.people, plan.people, locale))}
        </p>
      </div>
      <dl className="m-0 flex flex-col gap-1 border-t-2 border-dashed border-line pt-3">
        {budget.costs.map((cost) => (
          <div key={cost.category} className="flex justify-between gap-3">
            <dt>{text.plan.costs[cost.category]}</dt>
            <dd className="m-0 text-right tabular-nums">{formatTakaRange(cost.low, cost.high, locale)}</dd>
          </div>
        ))}
      </dl>
      {budget.notIncluded.length > 0 && (
        <div className="flex flex-col gap-1 text-muted">
          <h3 className="m-0 font-body text-sm font-semibold">{text.plan.notIncluded}</h3>
          <ul className="m-0 flex list-none flex-col gap-0.5 p-0 text-sm">
            {budget.notIncluded.map((missing, index) => (
              <li key={`${missing.category}-${index}`}>{missingLabel(missing, locale, text)}</li>
            ))}
          </ul>
        </div>
      )}
    </section>
  );
}

/** "Entry to Lalakhal", a bus service's name, or just the category, such as "Food". */
export function missingLabel(missing: MissingCost, locale: Locale, text: Dictionary): string {
  const name = localName(locale, missing);
  if (!name) {
    return text.plan.costs[missing.category];
  }
  return missing.category === "ENTRY_FEES" ? text.plan.notIncludedItem(name) : name;
}

type JourneyProps = {
  title: string;
  journey: Journey;
  hubName: (slug: string | null) => string;
  locale: Locale;
  text: Dictionary;
};

function JourneySection({ title, journey, hubName, locale, text }: JourneyProps) {
  if (journey.legs.length === 0) {
    return null;
  }
  const stops: RouteStop[] = journey.legs.map((leg) => ({
    name: hubName(leg.fromHub),
    legToNext: legSummary(leg, locale, text),
  }));
  stops.push({ name: hubName(journey.legs[journey.legs.length - 1].toHub) });

  return (
    <section className="flex flex-col gap-3">
      <h2 className="m-0 text-2xl">{title}</h2>
      <RouteLine stops={stops} />
    </section>
  );
}

export function legSummary(leg: Leg, locale: Locale, text: Dictionary): string {
  const mode = text.plan.modes[leg.mode as keyof Dictionary["plan"]["modes"]] ?? leg.mode ?? "";
  const fare =
    leg.fareMin !== null
      ? formatTakaRange(leg.fareMin, leg.fareMax ?? leg.fareMin, locale)
      : text.plan.fareUnknown;
  return `${mode} · ${formatDuration(leg.durationMinutes, locale, text)} · ${fare}`;
}

function Day({ day, locale, text }: { day: DayPlan; locale: Locale; text: Dictionary }) {
  const extras: { label: string; stop: Stop | null }[] = [
    { label: text.plan.eat, stop: day.meal },
    { label: text.plan.shop, stop: day.market },
    { label: text.plan.craft, stop: day.craft },
  ];
  const isEmpty = day.stops.length === 0 && extras.every((extra) => !extra.stop);

  return (
    <section className="flex flex-col gap-3">
      <h2 className="m-0 text-2xl">{text.plan.day(formatNumber(day.day, locale))}</h2>
      {isEmpty && <p className="m-0 text-muted">{text.plan.restDay}</p>}
      {day.stops.length > 0 && (
        <ol className="m-0 flex list-none flex-col gap-3 p-0">
          {day.stops.map(({ stop, travelMinutes }) => (
            <li key={stop.id} className="flex gap-3.5">
              <span aria-hidden="true" className="mt-2.5 size-3 flex-none rounded-full bg-accent" />
              <div className="flex flex-col">
                <span className="font-semibold">{localName(locale, stop)}</span>
                <span className="text-sm text-muted">{stopDetails(stop, travelMinutes, locale, text)}</span>
              </div>
            </li>
          ))}
        </ol>
      )}
      {extras.some((extra) => extra.stop) && (
        <dl className="m-0 flex flex-col gap-1">
          {extras.map(
            ({ label, stop }) =>
              stop && (
                <div key={label} className="flex gap-2">
                  <dt className="text-muted">{label}:</dt>
                  <dd className="m-0">{localName(locale, stop)}</dd>
                </div>
              ),
          )}
        </dl>
      )}
    </section>
  );
}

function stopDetails(stop: Stop, travelMinutes: number, locale: Locale, text: Dictionary): string {
  const details: string[] = [];
  if (travelMinutes > 0) {
    details.push(text.plan.travelThere(formatDuration(travelMinutes, locale, text)));
  }
  if (stop.visitMinutes) {
    details.push(text.plan.visitFor(formatDuration(stop.visitMinutes, locale, text)));
  }
  details.push(
    stop.feeMin !== null
      ? text.plan.fee(formatTakaRange(stop.feeMin, stop.feeMax ?? stop.feeMin, locale))
      : text.plan.free,
  );
  return details.join(" · ");
}
