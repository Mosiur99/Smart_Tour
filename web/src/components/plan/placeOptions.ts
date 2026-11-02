import type { DestinationSummary, Hub } from "@/api/types";
import type { PlaceOption } from "@/components/ui/PlaceSearch";
import { localName, type Locale } from "@/i18n/config";

type Named = Pick<Hub, "slug" | "nameEn" | "nameBn">;

export function toOption(place: Named, locale: Locale): PlaceOption {
  const other: Locale = locale === "bn" ? "en" : "bn";
  const name = localName(locale, place);
  const otherName = localName(other, place);
  return { id: place.slug ?? "", name, otherName: otherName === name ? undefined : otherName };
}

/** Matches either language, so "syl" and "সিলে" both find Sylhet. */
export function matchHubs(hubs: readonly Hub[], query: string, limit = 8): Hub[] {
  const search = query.trim().toLowerCase();
  if (!search) {
    return [];
  }
  return hubs
    .filter(
      (hub) =>
        hub.nameEn?.toLowerCase().includes(search) ||
        hub.nameBn?.includes(search) ||
        hub.slug?.includes(search),
    )
    .sort((a, b) => rank(a, search) - rank(b, search))
    .slice(0, limit);
}

/** Names that start with the query come first. */
function rank(hub: Hub, search: string): number {
  const starts = hub.nameEn?.toLowerCase().startsWith(search) || hub.nameBn?.startsWith(search);
  return starts ? 0 : 1;
}

export function destinationById(
  places: readonly DestinationSummary[],
  id: string,
): DestinationSummary | undefined {
  return places.find((place) => place.slug === id);
}
