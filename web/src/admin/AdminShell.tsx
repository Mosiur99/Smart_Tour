"use client";

import Link from "next/link";
import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from "react";
import { fetchHubs } from "@/api/browser";
import type { AdminUser, EntitySpec, Hub } from "@/api/types";
import { Notice } from "@/components/ui/Notice";
import { Skeleton } from "@/components/ui/Skeleton";
import { useAdminData } from "./useAdminData";

export type AdminContextValue = {
  user: AdminUser;
  specs: Record<string, EntitySpec>;
  hubs: Hub[];
};

const AdminContext = createContext<AdminContextValue | null>(null);

/** Who is signed in, the form fields and the hub list, loaded once for every admin page. */
export function useAdmin(): AdminContextValue {
  const value = useContext(AdminContext);
  if (!value) {
    throw new Error("useAdmin must be used inside AdminShell");
  }
  return value;
}

export function AdminProvider({ value, children }: { value: AdminContextValue; children: ReactNode }) {
  return <AdminContext.Provider value={value}>{children}</AdminContext.Provider>;
}

export function AdminShell({ children }: { children: ReactNode }) {
  const me = useAdminData<AdminUser>("/me");
  const schema = useAdminData<EntitySpec[]>("/schema");
  const [hubs, setHubs] = useState<Hub[]>([]);

  useEffect(() => {
    fetchHubs().then(setHubs, () => setHubs([]));
  }, []);

  const value = useMemo(() => {
    if (!me.data || !schema.data) {
      return null;
    }
    return {
      user: me.data,
      specs: Object.fromEntries(schema.data.map((spec) => [spec.entity, spec])),
      hubs,
    };
  }, [me.data, schema.data, hubs]);

  return (
    <>
      <header className="border-b border-line bg-surface print:hidden">
        <div className="mx-auto flex max-w-5xl flex-wrap items-center gap-x-6 gap-y-2 px-5 py-3">
          <Link href="/admin" className="font-display text-lg font-semibold text-ink no-underline">
            SmartTour admin
          </Link>
          <nav aria-label="Admin" className="flex gap-4">
            <Link href="/admin" className="text-accent">
              Overview
            </Link>
            <Link href="/admin/history" className="text-accent">
              History
            </Link>
          </nav>
          {me.data && (
            <p className="m-0 ms-auto text-sm text-muted">
              {me.data.email} · {me.data.role === "PUBLISHER" ? "Publisher" : "Editor"}
            </p>
          )}
        </div>
      </header>
      <main id="main" className="mx-auto w-full max-w-5xl flex-1 px-5 pt-6 pb-16">
        {me.failure ? (
          <Notice tone="problem">{me.failure.message}</Notice>
        ) : schema.failure ? (
          <Notice tone="problem">{schema.failure.message}</Notice>
        ) : value ? (
          <AdminProvider value={value}>{children}</AdminProvider>
        ) : (
          <Skeleton label="Loading" />
        )}
      </main>
    </>
  );
}
