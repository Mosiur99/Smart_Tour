import Link from "next/link";
import type { DestinationDetail, DestinationHotel, Place, TransportOption } from "@/api/types";
import { localName, localePath, type Locale } from "@/i18n/config";
import type { Dictionary } from "@/i18n/dictionary";
import { formatDuration } from "@/i18n/format";
import { formatTakaRange } from "@/lib/money";
import { TrustLabel } from "./TrustLabel";

type RouteLink = { href: string; label: string };

type ViewProps = { detail: DestinationDetail; routes: RouteLink[]; locale: Locale; text: Dictionary };

const SECTIONS = ["spots", "heritage", "food", "markets", "crafts"] as const;

export function DestinationView({ detail, routes, locale, text }: ViewProps) {
  const { summary } = detail;
  const name = localName(locale, summary);
  const division = localName(locale, { nameEn: summary.divisionNameEn, nameBn: summary.divisionNameBn });
  const sections = SECTIONS.filter((key) => detail.places[key].length > 0);
  const isEmpty = sections.length === 0 && detail.hotels.length === 0 && detail.transport.arriving.length === 0;

  return (
    <article className="flex flex-col gap-8">
      <header className="flex flex-col gap-2">
        <p className="m-0 text-muted">{text.destination.division(division)}</p>
        <h1 className="m-0 text-[32px] sm:text-[40px]">{name}</h1>
        {detail.travelNote && <p className="m-0">{detail.travelNote}</p>}
        <Link
          href={`${localePath(locale, "/")}?to=${summary.slug}`}
          className="mt-2 flex min-h-12 items-center justify-center self-start rounded-control bg-accent px-5 font-semibold text-on-accent no-underline hover:bg-accent-hover"
        >
          {text.destination.planHere(name)}
        </Link>
      </header>

      {isEmpty && <p className="m-0 text-muted">{text.destination.nothingYet}</p>}

      {sections.map((key) => (
        <section key={key} className="flex flex-col gap-3">
          <h2 className="m-0 text-2xl">{text.destination.sections[key]}</h2>
          <ul className="m-0 flex list-none flex-col gap-3 p-0">
            {detail.places[key].map((place) => (
              <PlaceItem key={place.nameEn} place={place} locale={locale} text={text} />
            ))}
          </ul>
        </section>
      ))}

      {detail.hotels.length > 0 && (
        <section className="flex flex-col gap-3">
          <h2 className="m-0 text-2xl">{text.destination.hotels}</h2>
          <ul className="m-0 flex list-none flex-col gap-3 p-0">
            {detail.hotels.map((hotel) => (
              <HotelItem key={hotel.nameEn} hotel={hotel} locale={locale} text={text} />
            ))}
          </ul>
          {detail.hotelNote && <p className="m-0 text-sm text-muted">{detail.hotelNote}</p>}
        </section>
      )}

      {detail.transport.arriving.length > 0 && (
        <section className="flex flex-col gap-3">
          <h2 className="m-0 text-2xl">{text.destination.gettingHere}</h2>
          <ul className="m-0 flex list-none flex-col gap-3 p-0">
            {detail.transport.arriving.map((option, index) => (
              <TransportItem key={`${option.serviceName}-${index}`} option={option} locale={locale} text={text} />
            ))}
          </ul>
          {routes.length > 0 && (
            <ul className="m-0 flex list-none flex-wrap gap-x-4 gap-y-1 p-0">
              {routes.map((route) => (
                <li key={route.href}>
                  <Link href={route.href} className="text-accent">
                    {route.label}
                  </Link>
                </li>
              ))}
            </ul>
          )}
        </section>
      )}
    </article>
  );
}

function PlaceItem({ place, locale, text }: { place: Place; locale: Locale; text: Dictionary }) {
  return (
    <li className="flex flex-col gap-1 rounded-card border border-line bg-surface p-4">
      <span className="font-semibold">{localName(locale, place)}</span>
      {place.note && <span className="text-muted">{place.note}</span>}
      {place.feeMin !== null && (
        <span className="text-sm">{text.plan.fee(formatTakaRange(place.feeMin, place.feeMax ?? place.feeMin, locale))}</span>
      )}
      <span>
        <TrustLabel verified={place.verified} checkedAt={place.checkedAt} locale={locale} text={text} />
      </span>
    </li>
  );
}

function HotelItem({ hotel, locale, text }: { hotel: DestinationHotel; locale: Locale; text: Dictionary }) {
  const price =
    hotel.priceMin !== null
      ? text.destination.perNight(formatTakaRange(hotel.priceMin, hotel.priceMax ?? hotel.priceMin, locale))
      : text.destination.priceUnknown;
  return (
    <li className="flex flex-col gap-1 rounded-card border border-line bg-surface p-4">
      <span className="font-semibold">{localName(locale, hotel)}</span>
      {hotel.address && <span className="text-sm text-muted">{hotel.address}</span>}
      <span>{price}</span>
      <span>
        <TrustLabel verified={hotel.verified} checkedAt={hotel.checkedAt} locale={locale} text={text} />
      </span>
    </li>
  );
}

export function TransportItem({
  option,
  locale,
  text,
}: {
  option: TransportOption;
  locale: Locale;
  text: Dictionary;
}) {
  const fare =
    option.fareMin !== null
      ? text.destination.fare(formatTakaRange(option.fareMin, option.fareMax ?? option.fareMin, locale))
      : text.plan.fareUnknown;
  return (
    <li className="flex flex-col gap-1 rounded-card border border-line bg-surface p-4">
      <span className="font-semibold">{option.serviceName}</span>
      <span className="text-sm text-muted">
        {[
          option.durationMinutes !== null &&
            text.route.duration(formatDuration(option.durationMinutes, locale, text)),
          fare,
          option.departureNote,
        ]
          .filter(Boolean)
          .join(" · ")}
      </span>
      <span>
        <TrustLabel verified={option.verified} checkedAt={option.checkedAt} locale={locale} text={text} />
      </span>
    </li>
  );
}
