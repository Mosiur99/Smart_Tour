import { TrustBadge } from "@/components/ui/TrustBadge";
import type { Locale } from "@/i18n/config";
import type { Dictionary } from "@/i18n/dictionary";
import { formatMonthYear } from "@/i18n/format";

type TrustLabelProps = {
  verified: boolean;
  checkedAt: string | null;
  locale: Locale;
  text: Dictionary;
};

/** Says in words whether a figure was checked, and when. */
export function TrustLabel({ verified, checkedAt, locale, text }: TrustLabelProps) {
  if (verified && checkedAt) {
    return (
      <TrustBadge trust="verified">{text.destination.verified(formatMonthYear(checkedAt, locale))}</TrustBadge>
    );
  }
  return <TrustBadge trust="stale">{text.destination.unverified}</TrustBadge>;
}
