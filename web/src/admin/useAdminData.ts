"use client";

import { useCallback, useEffect, useState } from "react";
import { adminRequest, type AdminFailure } from "./api";

type State<T> = { data: T | null; failure: AdminFailure | null; loading: boolean };

/** Loads one admin address and reloads it on request. */
export function useAdminData<T>(path: string) {
  const [state, setState] = useState<State<T>>({ data: null, failure: null, loading: true });
  const [attempt, setAttempt] = useState(0);

  useEffect(() => {
    let current = true;
    adminRequest<T>(path).then((result) => {
      if (current) {
        setState(
          result.ok
            ? { data: result.data, failure: null, loading: false }
            : { data: null, failure: result.failure, loading: false },
        );
      }
    });
    return () => {
      current = false;
    };
  }, [path, attempt]);

  const reload = useCallback(() => setAttempt((count) => count + 1), []);
  return { ...state, reload };
}
