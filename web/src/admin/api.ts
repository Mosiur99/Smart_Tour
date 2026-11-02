/**
 * Calls the admin API from the browser. Cloudflare Access adds the login to
 * each request on its way, so nothing here handles passwords or tokens.
 */
export type AdminFailure = {
  status: number;
  message: string;
  /** Field name to message, for problems that belong to one field. */
  fields: Record<string, string>;
};

export type AdminResult<T> = { ok: true; data: T } | { ok: false; failure: AdminFailure };

type Method = "GET" | "POST" | "PUT";

const FALLBACK_MESSAGES: Record<number, string> = {
  0: "Couldn't reach the server. Check the connection and try again.",
  401: "Your login has ended. Reload the page to sign in again.",
  403: "This account can't do that.",
  404: "That record doesn't exist any more.",
  409: "Someone saved this record after you opened it. Reload to see their change.",
};

export async function adminRequest<T>(
  path: string,
  method: Method = "GET",
  body?: unknown,
): Promise<AdminResult<T>> {
  let response: Response;
  try {
    response = await fetch(`/api/v1/admin${path}`, {
      method,
      cache: "no-store",
      headers: {
        accept: "application/json",
        ...(body === undefined ? {} : { "content-type": "application/json" }),
      },
      body: body === undefined ? undefined : JSON.stringify(body),
    });
  } catch {
    return { ok: false, failure: failure(0) };
  }
  if (response.ok) {
    return { ok: true, data: (await response.json()) as T };
  }
  return { ok: false, failure: await failureFrom(response) };
}

async function failureFrom(response: Response): Promise<AdminFailure> {
  try {
    const problem = (await response.json()) as { detail?: unknown; fields?: unknown };
    return failure(
      response.status,
      typeof problem.detail === "string" ? problem.detail : undefined,
      isMessageMap(problem.fields) ? problem.fields : {},
    );
  } catch {
    return failure(response.status);
  }
}

function failure(status: number, message?: string, fields: Record<string, string> = {}): AdminFailure {
  return {
    status,
    message: message ?? FALLBACK_MESSAGES[status] ?? "Something went wrong. Try again in a moment.",
    fields,
  };
}

function isMessageMap(value: unknown): value is Record<string, string> {
  return (
    typeof value === "object" &&
    value !== null &&
    Object.values(value).every((message) => typeof message === "string")
  );
}
