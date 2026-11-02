"use client";

import { useMemo, useState } from "react";
import { Button } from "@/components/ui/Button";
import { ChoiceGroup } from "@/components/ui/ChoiceGroup";
import { Notice } from "@/components/ui/Notice";
import { PlaceSearch, type PlaceOption } from "@/components/ui/PlaceSearch";
import { RouteLine } from "@/components/ui/RouteLine";
import { Skeleton } from "@/components/ui/Skeleton";
import { Stepper } from "@/components/ui/Stepper";
import { TextField } from "@/components/ui/TextField";
import { ThemeToggle } from "@/components/ui/ThemeToggle";
import { TrustBadge } from "@/components/ui/TrustBadge";
import { formatTakaRange, type Locale } from "@/lib/money";

const PLACES = [
  { id: "sreemangal", en: "Sreemangal", bn: "শ্রীমঙ্গল" },
  { id: "sreepur", en: "Sreepur", bn: "শ্রীপুর" },
  { id: "dhaka", en: "Dhaka", bn: "ঢাকা" },
  { id: "coxs-bazar", en: "Cox's Bazar", bn: "কক্সবাজার" },
];

const TEXT = {
  en: {
    title: "Three days in Sreemangal",
    intro: "Every component in the design system. All figures here are sample data.",
    switchLanguage: "বাংলা",
    toDark: "Dark theme",
    toLight: "Light theme",
    plan: "Plan my trip",
    share: "Share plan",
    change: "Change dates",
    where: "Where to?",
    noPlace: "No place found",
    note: "Note for the hotel",
    noteError: "Keep this under 200 letters.",
    style: "Trip style",
    styles: { budget: "Budget", standard: "Standard", comfort: "Comfort" },
    days: "Days",
    daysValue: (days: string) => `${days} days`,
    fewer: "Fewer days",
    more: "More days",
    verified: "Verified Oct 2026",
    stale: "Check before you go",
    excluded: "Not included",
    total: "Estimated total for 2 people",
    staleNotice: "Some prices are over six months old. Check them before you book.",
    failed: "We could not save your plan. Please try again.",
    loading: "Loading plan",
    origin: "Dhaka",
    leg: "Train, about 4 h 30 m",
  },
  bn: {
    title: "তিন দিনে শ্রীমঙ্গল",
    intro: "ডিজাইন সিস্টেমের সব উপাদান। এখানকার সব সংখ্যা নমুনা।",
    switchLanguage: "English",
    toDark: "ডার্ক থিম",
    toLight: "লাইট থিম",
    plan: "প্ল্যান তৈরি করুন",
    share: "শেয়ার করুন",
    change: "তারিখ বদলান",
    where: "কোথায় যেতে চান?",
    noPlace: "কোনো জায়গা পাওয়া যায়নি",
    note: "হোটেলের জন্য নোট",
    noteError: "২০০ অক্ষরের মধ্যে লিখুন।",
    style: "খরচের ধরন",
    styles: { budget: "কম খরচে", standard: "মাঝারি", comfort: "আরামে" },
    days: "কত দিন",
    daysValue: (days: string) => `${days} দিন`,
    fewer: "দিন কমান",
    more: "দিন বাড়ান",
    verified: "যাচাই করা হয়েছে অক্টোবর ২০২৬",
    stale: "যাওয়ার আগে দেখে নিন",
    excluded: "ধরা হয়নি",
    total: "২ জনের মোট আনুমানিক খরচ",
    staleNotice: "কিছু দাম ছয় মাসের বেশি পুরোনো। বুকিংয়ের আগে দেখে নিন।",
    failed: "প্ল্যানটি সংরক্ষণ করা যায়নি। আবার চেষ্টা করুন।",
    loading: "প্ল্যান লোড হচ্ছে",
    origin: "ঢাকা",
    leg: "ট্রেনে প্রায় ৪ ঘণ্টা ৩০ মিনিট",
  },
};

type TripStyle = keyof typeof TEXT.en.styles;

export function DesignPreview() {
  const [locale, setLocale] = useState<Locale>("en");
  const [query, setQuery] = useState("");
  const [days, setDays] = useState(3);
  const [tripStyle, setTripStyle] = useState<TripStyle>("standard");
  const text = TEXT[locale];

  const options: PlaceOption[] = useMemo(() => {
    const search = query.trim().toLowerCase();
    return PLACES.filter(
      (place) => place.en.toLowerCase().includes(search) || place.bn.includes(search),
    ).map((place) => ({
      id: place.id,
      name: place[locale],
      otherName: place[locale === "en" ? "bn" : "en"],
    }));
  }, [query, locale]);

  return (
    <div lang={locale} className="flex flex-col gap-10">
      <header className="flex flex-col gap-3">
        <div className="flex flex-wrap items-center justify-between gap-2">
          <span className="font-display text-xl font-semibold text-accent">SmartTour</span>
          <div className="flex flex-wrap gap-1">
            <Button variant="quiet" onClick={() => setLocale(locale === "en" ? "bn" : "en")}>
              {text.switchLanguage}
            </Button>
            <ThemeToggle toDarkLabel={text.toDark} toLightLabel={text.toLight} />
          </div>
        </div>
        <h1 className="m-0 text-[32px]">{text.title}</h1>
        <p className="m-0 text-muted">{text.intro}</p>
      </header>

      <section className="flex flex-wrap gap-3">
        <Button>{text.plan}</Button>
        <Button variant="secondary">{text.share}</Button>
        <Button variant="quiet">{text.change}</Button>
      </section>

      <section className="flex flex-col gap-4">
        <PlaceSearch
          label={text.where}
          query={query}
          onQueryChange={setQuery}
          options={options}
          onSelect={(option) => setQuery(option.name)}
          noResultsText={text.noPlace}
        />
        <TextField label={text.note} error={text.noteError} defaultValue="" />
      </section>

      <section className="flex flex-col gap-4">
        <ChoiceGroup
          label={text.style}
          value={tripStyle}
          onChange={setTripStyle}
          choices={(Object.keys(text.styles) as TripStyle[]).map((value) => ({
            value,
            label: text.styles[value],
          }))}
        />
        <Stepper
          label={text.days}
          value={days}
          valueText={text.daysValue(new Intl.NumberFormat(locale).format(days))}
          min={1}
          max={14}
          onChange={setDays}
          decreaseLabel={text.fewer}
          increaseLabel={text.more}
        />
      </section>

      <section className="flex flex-wrap gap-2">
        <TrustBadge trust="verified">{text.verified}</TrustBadge>
        <TrustBadge trust="stale">{text.stale}</TrustBadge>
        <TrustBadge trust="excluded">{text.excluded}</TrustBadge>
      </section>

      <section className="flex flex-col gap-1 rounded-card border border-line bg-surface p-5">
        <span className="text-sm text-muted">{text.total}</span>
        <span className="font-display text-[28px] leading-tight font-semibold">
          {formatTakaRange(8400, 13600, locale)}
        </span>
      </section>

      <RouteLine stops={[{ name: text.origin, legToNext: text.leg }, { name: PLACES[0][locale] }]} />

      <section className="flex flex-col gap-3">
        <Notice tone="caution">{text.staleNotice}</Notice>
        <Notice tone="problem">{text.failed}</Notice>
        <Skeleton width="70%" label={text.loading} />
        <Skeleton width="45%" />
      </section>
    </div>
  );
}
