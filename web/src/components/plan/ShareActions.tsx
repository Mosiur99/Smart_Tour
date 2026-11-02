"use client";

import { useState } from "react";
import { Button } from "@/components/ui/Button";
import type { Dictionary } from "@/i18n/dictionary";

type ShareActionsProps = { title: string; shareText: string; text: Dictionary["share"] };

/** One tap to share: the phone's share sheet where there is one, else a copied link. */
export function ShareActions({ title, shareText, text }: ShareActionsProps) {
  const [copied, setCopied] = useState(false);

  async function share() {
    const url = window.location.href;
    if (navigator.share) {
      try {
        await navigator.share({ title, text: shareText, url });
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
          {text.share}
        </Button>
        <Button variant="secondary" onClick={() => window.print()} className="flex-1">
          {text.print}
        </Button>
      </div>
      <p role="status" className="m-0 min-h-6 text-sm text-muted">
        {copied ? text.copied : ""}
      </p>
    </div>
  );
}
