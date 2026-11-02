"use client";

import { useState } from "react";
import { Button } from "@/components/ui/Button";
import type { Locale } from "@/i18n/config";
import { dictionary } from "@/i18n/dictionary";

/** One tap to share: the phone's share sheet where there is one, else a copied link. */
export function ShareActions({ locale, title }: { locale: Locale; title: string }) {
  const text = dictionary(locale);
  const [copied, setCopied] = useState(false);

  async function share() {
    const url = window.location.href;
    if (navigator.share) {
      try {
        await navigator.share({ title, text: text.plan.shareText(title), url });
      } catch {
        // The visitor closed the share sheet. Nothing to do.
      }
      return;
    }
    await navigator.clipboard.writeText(url);
    setCopied(true);
  }

  return (
    <div className="flex flex-col gap-2 print:hidden">
      <div className="flex flex-wrap gap-3">
        <Button onClick={share} className="flex-1">
          {text.plan.share}
        </Button>
        <Button variant="secondary" onClick={() => window.print()} className="flex-1">
          {text.plan.print}
        </Button>
      </div>
      <p role="status" className="m-0 min-h-6 text-sm text-muted">
        {copied ? text.plan.copied : ""}
      </p>
    </div>
  );
}
