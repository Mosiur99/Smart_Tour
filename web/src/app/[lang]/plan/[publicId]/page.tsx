import type { Metadata } from "next";
import { notFound } from "next/navigation";
import { cache } from "react";
import { getHubs, getPlan } from "@/api/server";
import type { Hub } from "@/api/types";
import { PlanView, planTitle } from "@/components/plan/PlanView";
import { isLocale, localName, type Locale } from "@/i18n/config";
import { dictionary } from "@/i18n/dictionary";
import { alternates } from "@/lib/seo";

// No plans are built ahead of time. Each one renders on its first visit and
// is then cached, because a saved plan never changes.
export function generateStaticParams() {
  return [];
}

// One API call per request, shared by the metadata and the page.
const loadPlan = cache(getPlan);

type Props = PageProps<"/[lang]/plan/[publicId]">;

export async function generateMetadata({ params }: Props): Promise<Metadata> {
  const { lang, publicId } = await params;
  const saved = isLocale(lang) ? await loadPlan(publicId) : null;
  if (!saved?.plan || !isLocale(lang)) {
    return {};
  }
  const text = dictionary(lang);
  const place = localName(lang, { nameEn: saved.plan.destinationNameEn, nameBn: saved.plan.destinationNameBn });
  return {
    title: planTitle(saved.plan, lang, text),
    description: text.plan.metaDescription(place),
    alternates: alternates(lang, `/plan/${publicId}`),
    // Plans are personal and near duplicates of each other, so search engines skip them.
    robots: { index: false, follow: true },
  };
}

export default async function PlanPage({ params }: Props) {
  const { lang, publicId } = await params;
  if (!isLocale(lang)) {
    notFound();
  }
  const [saved, hubs] = await Promise.all([loadPlan(publicId), getHubs()]);
  if (!saved?.plan || !saved.request) {
    notFound();
  }
  const text = dictionary(lang);
  const hubName = hubNamer(hubs, lang);

  return (
    <PlanView
      plan={saved.plan}
      fromName={hubName(saved.request.originHub)}
      budgetName={text.form.budgets[saved.request.budget]}
      hubName={hubName}
      locale={lang}
      text={text}
    />
  );
}

function hubNamer(hubs: Hub[], locale: Locale) {
  const bySlug = new Map(hubs.map((hub) => [hub.slug, hub]));
  return (slug: string | null) => {
    const hub = slug ? bySlug.get(slug) : undefined;
    return hub ? localName(locale, hub) : (slug ?? "");
  };
}
