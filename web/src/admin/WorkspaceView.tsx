"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import type { AdminRecord, Workspace } from "@/api/types";
import { Button } from "@/components/ui/Button";
import { Notice } from "@/components/ui/Notice";
import { Skeleton } from "@/components/ui/Skeleton";
import { formatTakaRange } from "@/lib/money";
import { useAdmin } from "./AdminShell";
import { adminRequest, type AdminFailure } from "./api";
import { newRecordValues } from "./formValues";
import { RecordCard } from "./RecordCard";
import { RecordForm } from "./RecordForm";
import { useAdminData } from "./useAdminData";

type Group = "places" | "hotels" | "legs";

/** One destination with its places, hotels and journeys, each editable in place. */
export function WorkspaceView({ slug }: { slug: string }) {
  const { data, failure, loading } = useAdminData<Workspace>(`/destinations/${slug}`);
  const [workspace, setWorkspace] = useState<Workspace | null>(null);

  useEffect(() => setWorkspace(data), [data]);

  if (loading) {
    return <Skeleton label="Loading" />;
  }
  if (failure || !workspace) {
    return <Notice tone="problem">{failure?.message ?? "This destination couldn't be loaded."}</Notice>;
  }

  function replace(group: Group, record: AdminRecord) {
    setWorkspace((current) =>
      current && {
        ...current,
        [group]: current[group].map((item) => (item.id === record.id ? record : item)),
      },
    );
  }

  function add(group: Group, record: AdminRecord) {
    setWorkspace((current) => current && { ...current, [group]: [...current[group], record] });
  }

  return (
    <div className="flex flex-col gap-10">
      <div className="flex flex-col gap-3">
        <Link href="/admin" className="text-accent">
          All destinations
        </Link>
        <h1 className="m-0 text-3xl">{workspace.destination.label}</h1>
        <p className="m-0 text-muted">
          Changes to published or imported records show on the site within a minute. Drafts stay hidden
          until they&apos;re published.
        </p>
        <RecordCard
          record={workspace.destination}
          headingLevel={2}
          onChanged={(record) => setWorkspace((current) => current && { ...current, destination: record })}
        />
      </div>
      <RecordGroup
        title="Places"
        entity="places"
        noun="place"
        slug={slug}
        records={workspace.places}
        detailOf={(record) => kindName(record.values.kind)}
        onChanged={(record) => replace("places", record)}
        onAdded={(record) => add("places", record)}
      />
      <RecordGroup
        title="Hotels"
        entity="hotels"
        noun="hotel"
        slug={slug}
        records={workspace.hotels}
        detailOf={(record) => priceRange(record.values.priceMin, record.values.priceMax, "a night")}
        onChanged={(record) => replace("hotels", record)}
        onAdded={(record) => add("hotels", record)}
      />
      <RecordGroup
        title="Journeys"
        entity="legs"
        noun="journey"
        slug={slug}
        records={workspace.legs}
        detailOf={(record) => priceRange(record.values.fareMin, record.values.fareMax, "a person")}
        onChanged={(record) => replace("legs", record)}
        onAdded={(record) => add("legs", record)}
      />
    </div>
  );
}

type RecordGroupProps = {
  title: string;
  entity: Group;
  noun: string;
  slug: string;
  records: AdminRecord[];
  detailOf: (record: AdminRecord) => string | undefined;
  onChanged: (record: AdminRecord) => void;
  onAdded: (record: AdminRecord) => void;
};

function RecordGroup({ title, entity, noun, slug, records, detailOf, onChanged, onAdded }: RecordGroupProps) {
  const headingId = `${entity}-heading`;
  return (
    <section aria-labelledby={headingId} className="flex flex-col gap-3">
      <div className="flex flex-wrap items-center gap-3">
        <h2 id={headingId} className="m-0 text-2xl">
          {title}
        </h2>
        <span className="text-muted">{records.length}</span>
      </div>
      <NewRecord entity={entity} noun={noun} slug={slug} onAdded={onAdded} />
      {records.length === 0 ? (
        <p className="m-0 text-muted">None yet.</p>
      ) : (
        records.map((record) => (
          <RecordCard key={record.id} record={record} detail={detailOf(record)} onChanged={onChanged} />
        ))
      )}
    </section>
  );
}

type NewRecordProps = { entity: Group; noun: string; slug: string; onAdded: (record: AdminRecord) => void };

/** New records start as drafts, so adding one never changes the site. */
function NewRecord({ entity, noun, slug, onAdded }: NewRecordProps) {
  const { specs, hubs } = useAdmin();
  const [open, setOpen] = useState(false);
  const [busy, setBusy] = useState(false);
  const [failure, setFailure] = useState<AdminFailure | null>(null);

  async function create(values: Record<string, unknown>) {
    setBusy(true);
    setFailure(null);
    const filled = Object.fromEntries(Object.entries(values).filter(([, value]) => value !== null));
    const result = await adminRequest<AdminRecord>(`/${entity}`, "POST", { destinationSlug: slug, values: filled });
    setBusy(false);
    if (result.ok) {
      onAdded(result.data);
      setOpen(false);
    } else {
      setFailure(result.failure);
    }
  }

  if (!open) {
    return (
      <Button variant="secondary" className="self-start" onClick={() => setOpen(true)}>
        Add a {noun}
      </Button>
    );
  }
  return (
    <div className="flex flex-col gap-3 rounded-card border border-stitch bg-surface p-4">
      <h3 className="m-0 text-lg">New {noun}</h3>
      {failure && <Notice tone="problem">{failure.message}</Notice>}
      <RecordForm
        fields={specs[entity]?.fields ?? []}
        values={newRecordValues(entity)}
        hubs={hubs}
        creating
        busy={busy}
        errors={failure?.fields ?? {}}
        submitLabel={`Add ${noun} as a draft`}
        onSubmit={create}
        onCancel={() => {
          setOpen(false);
          setFailure(null);
        }}
      />
    </div>
  );
}

function kindName(kind: unknown): string | undefined {
  return typeof kind === "string" ? kind.charAt(0) + kind.slice(1).toLowerCase() : undefined;
}

function priceRange(min: unknown, max: unknown, per: string): string {
  if (typeof min !== "number" || typeof max !== "number") {
    return "Price not known";
  }
  return `${formatTakaRange(min, max, "en")} ${per}`;
}
