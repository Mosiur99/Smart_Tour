"use client";

import { useState } from "react";
import type { AdminRecord, TargetStatus } from "@/api/types";
import { Button } from "@/components/ui/Button";
import { Notice } from "@/components/ui/Notice";
import { TrustBadge } from "@/components/ui/TrustBadge";
import { useAdmin } from "./AdminShell";
import { adminRequest, type AdminFailure } from "./api";
import { HistoryList } from "./HistoryList";
import { RecordForm } from "./RecordForm";
import { checkedLabel, editedValues, STATUS_LABELS } from "./words";

const VISIBLE = ["PUBLISHED", "IMPORTED"];
const STALE_AFTER_DAYS = 90;
const DAY = 24 * 60 * 60 * 1000;

type Mode = "closed" | "editing" | "history";

type RecordCardProps = {
  record: AdminRecord;
  /** Shown under the name, such as the kind of place. */
  detail?: string;
  /** Follows the heading the card sits under, so headings never skip a level. */
  headingLevel?: 2 | 3;
  onChanged: (record: AdminRecord) => void;
};

/** One record: its state at a glance, and editing, publishing and history on demand. */
export function RecordCard({ record, detail, headingLevel = 3, onChanged }: RecordCardProps) {
  const { user, specs, hubs } = useAdmin();
  const [mode, setMode] = useState<Mode>("closed");
  const [busy, setBusy] = useState(false);
  const [failure, setFailure] = useState<AdminFailure | null>(null);
  const [saved, setSaved] = useState<string | null>(null);

  const publisher = user.role === "PUBLISHER";
  const canEdit = publisher || !VISIBLE.includes(record.status);
  const checkedAt = typeof record.values.checkedAt === "string" ? record.values.checkedAt : null;
  const path = `/${record.entity}/${record.id}`;
  const Heading = headingLevel === 2 ? "h2" : "h3";

  async function run(request: Promise<{ ok: true; data: AdminRecord } | { ok: false; failure: AdminFailure }>, done: string) {
    setBusy(true);
    setFailure(null);
    setSaved(null);
    const result = await request;
    setBusy(false);
    if (result.ok) {
      onChanged(result.data);
      setMode("closed");
      setSaved(done);
    } else {
      setFailure(result.failure);
    }
  }

  function save(values: Record<string, unknown>) {
    const changed = editedValues(record.values, values);
    if (Object.keys(changed).length === 0) {
      setMode("closed");
      return;
    }
    void run(adminRequest<AdminRecord>(path, "PUT", { revision: record.revision, values: changed }), "Saved.");
  }

  function changeStatus(status: TargetStatus, done: string) {
    void run(adminRequest<AdminRecord>(`${path}/status`, "POST", { revision: record.revision, status }), done);
  }

  return (
    <article className="flex flex-col gap-3 rounded-card border border-line bg-surface p-4">
      <div className="flex flex-wrap items-start gap-x-4 gap-y-2">
        <div className="flex min-w-0 flex-1 flex-col gap-1">
          <Heading className="m-0 text-base">{record.label}</Heading>
          <div className="flex flex-wrap items-center gap-2 text-sm text-muted">
            <StatusBadge status={record.status} />
            {detail && <span>{detail}</span>}
            {checkedAt && <CheckedBadge isoDate={checkedAt} />}
          </div>
        </div>
        {mode === "closed" && (
          <div className="flex flex-wrap items-start gap-2">
            {publisher && record.status !== "PUBLISHED" && (
              <Button className="min-h-11 px-4" busy={busy} onClick={() => changeStatus("PUBLISHED", "Published.")}>
                Publish
              </Button>
            )}
            <Button variant="secondary" className="min-h-11 px-4" onClick={() => setMode("editing")}>
              {canEdit ? "Edit" : "View"}
            </Button>
            <Button variant="quiet" className="min-h-11 px-3" onClick={() => setMode("history")}>
              History
            </Button>
            {publisher && <MoreActions status={record.status} busy={busy} onChange={changeStatus} />}
          </div>
        )}
      </div>

      {saved && mode === "closed" && (
        <p role="status" className="m-0 text-sm text-soft-ink">
          {saved}
        </p>
      )}
      {failure && (
        <div className="flex flex-col gap-2">
          <Notice tone="problem">{failure.message}</Notice>
          {failure.status === 409 && (
            <Button variant="secondary" className="self-start" onClick={() => window.location.reload()}>
              Reload
            </Button>
          )}
        </div>
      )}

      {mode === "editing" && (
        <>
          {!canEdit && (
            <p className="m-0 text-sm text-muted">Only a publisher can change what visitors see.</p>
          )}
          <RecordForm
            fields={specs[record.entity]?.fields ?? []}
            values={record.values}
            hubs={hubs}
            creating={false}
            readOnly={!canEdit}
            busy={busy}
            errors={failure?.fields ?? {}}
            submitLabel="Save changes"
            onSubmit={save}
            onCancel={() => {
              setMode("closed");
              setFailure(null);
            }}
          />
        </>
      )}

      {mode === "history" && (
        <div className="flex flex-col gap-3">
          <HistoryList query={`?kind=${record.entity}&id=${record.id}`} showRecord={false} />
          <Button variant="secondary" className="self-start" onClick={() => setMode("closed")}>
            Close history
          </Button>
        </div>
      )}
    </article>
  );
}

type MoreActionsProps = {
  status: string;
  busy: boolean;
  onChange: (status: TargetStatus, done: string) => void;
};

/** Taking something off the site is rare, so it waits behind one more tap. */
function MoreActions({ status, busy, onChange }: MoreActionsProps) {
  return (
    <details className="group">
      <summary className="flex min-h-11 cursor-pointer list-none items-center rounded-control px-3 font-semibold text-accent hover:bg-sunk [&::-webkit-details-marker]:hidden">
        More
      </summary>
      <div className="flex flex-wrap gap-2 pt-2">
        {status === "PUBLISHED" && (
          <Button
            variant="secondary"
            className="min-h-11 px-4"
            busy={busy}
            onClick={() => onChange("DRAFT", "Unpublished. It's a draft again.")}
          >
            Unpublish
          </Button>
        )}
        {status !== "ARCHIVED" && (
          <Button variant="secondary" className="min-h-11 px-4" busy={busy} onClick={() => onChange("ARCHIVED", "Archived.")}>
            Archive
          </Button>
        )}
      </div>
    </details>
  );
}

export function StatusBadge({ status }: { status: string }) {
  const trust = status === "PUBLISHED" ? "verified" : status === "IMPORTED" ? "stale" : "excluded";
  return <TrustBadge trust={trust}>{STATUS_LABELS[status] ?? status}</TrustBadge>;
}

function CheckedBadge({ isoDate }: { isoDate: string }) {
  const old = Date.now() - new Date(`${isoDate}T00:00:00`).getTime() > STALE_AFTER_DAYS * DAY;
  return <span className={old ? "font-semibold text-caution" : undefined}>Checked {checkedLabel(isoDate)}</span>;
}
