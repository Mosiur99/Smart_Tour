"use client";

import Link from "next/link";
import { useState } from "react";
import type { DestinationOverview, StaleRecord } from "@/api/types";
import { Notice } from "@/components/ui/Notice";
import { Skeleton } from "@/components/ui/Skeleton";
import { TextField } from "@/components/ui/TextField";
import { StatusBadge } from "./RecordCard";
import { useAdminData } from "./useAdminData";
import { checkedLabel, KIND_LABELS } from "./words";

/** The admin home: prices that need checking first, then every destination. */
export function Overview() {
  return (
    <div className="flex flex-col gap-10">
      <StaleSection />
      <DestinationsSection />
    </div>
  );
}

function StaleSection() {
  const { data, failure, loading } = useAdminData<StaleRecord[]>("/stale");

  return (
    <section aria-labelledby="stale-heading" className="flex flex-col gap-3">
      <h1 id="stale-heading" className="m-0 text-2xl">
        Needs checking
      </h1>
      <p className="m-0 text-muted">Published details last checked more than 90 days ago, oldest first.</p>
      {loading ? (
        <Skeleton label="Loading" />
      ) : failure ? (
        <Notice tone="problem">{failure.message}</Notice>
      ) : !data?.length ? (
        <p className="m-0 rounded-card bg-soft px-4 py-3 text-soft-ink">Everything published was checked recently.</p>
      ) : (
        <ul className="m-0 flex list-none flex-col divide-y divide-line rounded-card border border-line bg-surface p-0">
          {data.map((item) => (
            <li key={`${item.entity}-${item.id}`} className="flex flex-wrap items-baseline gap-x-3 px-4 py-3">
              {item.destinationSlug ? (
                <Link href={`/admin/destinations/${item.destinationSlug}`} className="font-semibold text-accent">
                  {item.label}
                </Link>
              ) : (
                <span className="font-semibold">{item.label}</span>
              )}
              <span className="text-sm text-muted">{KIND_LABELS[item.entity]}</span>
              <span className="ms-auto text-sm text-caution">Checked {checkedLabel(item.checkedAt)}</span>
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}

function DestinationsSection() {
  const { data, failure, loading } = useAdminData<DestinationOverview[]>("/destinations");
  const [filter, setFilter] = useState("");

  const needle = filter.trim().toLowerCase();
  const shown = (data ?? []).filter(
    (destination) =>
      !needle ||
      destination.nameEn.toLowerCase().includes(needle) ||
      (destination.nameBn ?? "").includes(filter.trim()),
  );

  return (
    <section aria-labelledby="destinations-heading" className="flex flex-col gap-3">
      <h2 id="destinations-heading" className="m-0 text-2xl">
        Destinations
      </h2>
      <TextField
        label="Find a destination"
        type="search"
        value={filter}
        onChange={(event) => setFilter(event.target.value)}
        className="max-w-sm"
      />
      {loading ? (
        <Skeleton label="Loading" />
      ) : failure ? (
        <Notice tone="problem">{failure.message}</Notice>
      ) : (
        <ul className="m-0 grid list-none gap-3 p-0 sm:grid-cols-2">
          {shown.map((destination) => (
            <li key={destination.slug}>
              <Link
                href={`/admin/destinations/${destination.slug}`}
                className="flex h-full flex-col gap-1.5 rounded-card border border-line bg-surface px-4 py-3 text-ink no-underline hover:border-stitch"
              >
                <span className="flex flex-wrap items-center gap-2">
                  <span className="font-semibold">{destination.nameEn}</span>
                  {destination.nameBn && (
                    <span lang="bn" className="text-muted">
                      {destination.nameBn}
                    </span>
                  )}
                </span>
                <span className="flex flex-wrap items-center gap-2 text-sm text-muted">
                  <StatusBadge status={destination.status} />
                  {destination.published} of {destination.records} published
                  {destination.stale > 0 && <span className="text-caution">· {destination.stale} to check</span>}
                </span>
              </Link>
            </li>
          ))}
        </ul>
      )}
      {data && shown.length === 0 && <p className="m-0 text-muted">No destination matches “{filter}”.</p>}
    </section>
  );
}
