"use client";

import Link from "next/link";
import type { AuditEntry } from "@/api/types";
import { Notice } from "@/components/ui/Notice";
import { Skeleton } from "@/components/ui/Skeleton";
import { useAdminData } from "./useAdminData";
import { actionLabel, changesIn, displayValue, fieldLabel, KIND_LABELS, timeLabel } from "./words";

type HistoryListProps = {
  /** Narrows the list, such as "?kind=hotels&id=4". */
  query?: string;
  /** Names the record on each entry, for the list of all changes. */
  showRecord?: boolean;
};

/** Who changed what and when, with each field's old and new value. */
export function HistoryList({ query = "", showRecord = true }: HistoryListProps) {
  const { data, failure, loading } = useAdminData<AuditEntry[]>(`/history${query}`);

  if (loading) {
    return <Skeleton label="Loading history" />;
  }
  if (failure) {
    return <Notice tone="problem">{failure.message}</Notice>;
  }
  if (!data || data.length === 0) {
    return <p className="m-0 text-muted">No changes yet.</p>;
  }
  return (
    <ol className="m-0 flex list-none flex-col gap-3 p-0">
      {data.map((entry) => (
        <li key={entry.id} className="rounded-card bg-sunk px-4 py-3">
          <p className="m-0">
            <strong>{actionLabel(entry.action)}</strong>
            {showRecord && (
              <>
                {" "}
                {KIND_LABELS[entry.entity]?.toLowerCase() ?? entry.entity}{" "}
                <RecordName entry={entry} />
              </>
            )}{" "}
            <span className="text-muted">
              by {entry.actor}, {timeLabel(entry.at)}
            </span>
          </p>
          <ChangeList entry={entry} />
        </li>
      ))}
    </ol>
  );
}

function RecordName({ entry }: { entry: AuditEntry }) {
  const name = entry.after?.nameEn ?? entry.after?.serviceName ?? entry.before?.nameEn ?? `#${entry.entityId}`;
  return <span className="font-semibold">{String(name)}</span>;
}

function ChangeList({ entry }: { entry: AuditEntry }) {
  if (entry.action === "CREATE") {
    return null;
  }
  const changes = changesIn(entry);
  if (changes.length === 0) {
    return null;
  }
  return (
    <ul className="m-0 mt-1 flex flex-col gap-0.5 ps-5 text-sm">
      {changes.map((change) => (
        <li key={change.field}>
          {fieldLabel(change.field)}: <s className="text-muted">{displayValue(change.before)}</s> to{" "}
          {displayValue(change.after)}
        </li>
      ))}
    </ul>
  );
}

export function HistoryPage() {
  return (
    <section className="flex flex-col gap-4">
      <h1 className="m-0 text-2xl">History</h1>
      <p className="m-0 text-muted">
        The latest 100 changes. Each one shows the old and new values, so any change can be put back
        by hand. Go to a <Link href="/admin">destination</Link> to change a record.
      </p>
      <HistoryList />
    </section>
  );
}
