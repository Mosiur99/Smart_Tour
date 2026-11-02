import type { Metadata } from "next";
import { notFound } from "next/navigation";
import { Suspense } from "react";
import { PlanForm } from "@/components/plan/PlanForm";
import { isLocale } from "@/i18n/config";
import { dictionary } from "@/i18n/dictionary";
import { alternates } from "@/lib/seo";

export async function generateMetadata({ params }: PageProps<"/[lang]">): Promise<Metadata> {
  const { lang } = await params;
  if (!isLocale(lang)) {
    return {};
  }
  const text = dictionary(lang);
  return {
    title: { absolute: text.home.metaTitle },
    description: text.site.tagline,
    alternates: alternates(lang, "/"),
  };
}

export default async function Home({ params }: PageProps<"/[lang]">) {
  const { lang } = await params;
  if (!isLocale(lang)) {
    notFound();
  }
  const text = dictionary(lang);

  return (
    <div className="flex flex-col gap-8 pt-4">
      <div className="flex flex-col gap-2">
        <h1 className="m-0 text-[32px] sm:text-[40px]">{text.home.title}</h1>
        <p className="m-0 text-muted">{text.home.intro}</p>
      </div>
      {/* The form reads ?to= from the address, which needs a Suspense boundary on a static page. */}
      <Suspense>
        <PlanForm locale={lang} />
      </Suspense>
    </div>
  );
}
