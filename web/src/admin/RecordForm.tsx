"use client";

import { useId, useState, type FormEvent, type ReactNode } from "react";
import type { FieldSpec, Hub } from "@/api/types";
import { Button } from "@/components/ui/Button";
import { TextField } from "@/components/ui/TextField";
import { fromDraft, toDraft, type Draft, type DraftValue } from "./formValues";
import { fieldHint, fieldLabel, MONTHS, todayIso } from "./words";

type RecordFormProps = {
  fields: FieldSpec[];
  values: Record<string, unknown>;
  hubs: Hub[];
  creating: boolean;
  readOnly?: boolean;
  busy: boolean;
  errors: Record<string, string>;
  submitLabel: string;
  onSubmit: (values: Record<string, unknown>) => void;
  onCancel: () => void;
};

/** One form for every kind of record, built from the fields the API describes. */
export function RecordForm({
  fields,
  values,
  hubs,
  creating,
  readOnly = false,
  busy,
  errors,
  submitLabel,
  onSubmit,
  onCancel,
}: RecordFormProps) {
  const [draft, setDraft] = useState<Draft>(() => toDraft(fields, values));

  function change(name: string, value: DraftValue) {
    setDraft((current) => ({ ...current, [name]: value }));
  }

  function submit(event: FormEvent) {
    event.preventDefault();
    onSubmit(fromDraft(fields, draft));
  }

  return (
    <form onSubmit={submit} noValidate className="flex flex-col gap-5">
      <fieldset disabled={readOnly || busy} className="m-0 grid gap-4 border-0 p-0 sm:grid-cols-2">
        {fields.map((field) => (
          <FieldInput
            key={field.name}
            field={field}
            value={draft[field.name] ?? ""}
            hubs={hubs}
            locked={field.createOnly && !creating}
            error={errors[field.name]}
            onChange={(value) => change(field.name, value)}
          />
        ))}
      </fieldset>
      <div className="flex flex-wrap gap-3">
        {!readOnly && (
          <Button type="submit" busy={busy}>
            {submitLabel}
          </Button>
        )}
        <Button variant="secondary" onClick={onCancel}>
          {readOnly ? "Close" : "Cancel"}
        </Button>
      </div>
    </form>
  );
}

type FieldInputProps = {
  field: FieldSpec;
  value: DraftValue;
  hubs: Hub[];
  locked: boolean;
  error?: string;
  onChange: (value: DraftValue) => void;
};

function FieldInput({ field, value, hubs, locked, error, onChange }: FieldInputProps) {
  // Most fields can stay empty, so the few that can't are the ones marked.
  const label = fieldLabel(field.name) + (field.required ? " (required)" : "");
  const hint = fieldHint(field.name);
  const wide = field.type === "LONG_TEXT" || field.type === "NUMBERS";

  if (Array.isArray(value)) {
    return <NumberChoices field={field} label={label} picked={value} error={error} onChange={onChange} />;
  }
  if (field.type === "CHOICE" || field.type === "HUB") {
    const options =
      field.type === "HUB"
        ? hubs.map((hub) => ({ value: hub.slug, label: hub.nameEn }))
        : field.options.map((option) => ({ value: option, label: option.charAt(0) + option.slice(1).toLowerCase() }));
    return (
      <Labelled label={label} hint={hint} error={error}>
        {(id, describedBy) => (
          <select
            id={id}
            value={value}
            disabled={locked}
            aria-invalid={error ? true : undefined}
            aria-describedby={describedBy}
            onChange={(event) => onChange(event.target.value)}
            className={inputClass(error)}
          >
            <option value="">{field.required ? "Choose" : "None"}</option>
            {options.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>
        )}
      </Labelled>
    );
  }
  if (field.type === "LONG_TEXT") {
    return (
      <Labelled label={label} hint={hint} error={error} wide>
        {(id, describedBy) => (
          <textarea
            id={id}
            value={value}
            rows={3}
            aria-invalid={error ? true : undefined}
            aria-describedby={describedBy}
            onChange={(event) => onChange(event.target.value)}
            className={`${inputClass(error)} py-2.5`}
          />
        )}
      </Labelled>
    );
  }
  if (field.type === "DATE") {
    return (
      <div className="flex flex-col gap-1">
        <TextField
          type="date"
          label={label}
          hint={hint}
          error={error}
          value={value}
          max={todayIso()}
          onChange={(event) => onChange(event.target.value)}
        />
        <Button variant="quiet" className="min-h-11 self-start px-1" onClick={() => onChange(todayIso())}>
          Checked today
        </Button>
      </div>
    );
  }
  const numeric = field.type === "INTEGER" || field.type === "DECIMAL";
  return (
    <TextField
      className={wide ? "sm:col-span-2" : ""}
      label={label}
      hint={hint}
      error={error}
      value={value}
      readOnly={locked}
      type={field.type === "URL" ? "url" : numeric ? "number" : "text"}
      inputMode={field.type === "INTEGER" ? "numeric" : field.type === "DECIMAL" ? "decimal" : undefined}
      step={field.type === "DECIMAL" ? "any" : undefined}
      min={field.min ?? undefined}
      max={field.max ?? undefined}
      lang={field.name.endsWith("Bn") ? "bn" : undefined}
      onChange={(event) => onChange(event.target.value)}
    />
  );
}

type NumberChoicesProps = {
  field: FieldSpec;
  label: string;
  picked: number[];
  error?: string;
  onChange: (value: number[]) => void;
};

/** Months, or any short list of numbers, as boxes to tick. */
function NumberChoices({ field, label, picked, error, onChange }: NumberChoicesProps) {
  const errorId = useId();
  const first = field.min ?? 1;
  const last = field.max ?? 12;
  const numbers = Array.from({ length: last - first + 1 }, (_, index) => first + index);
  const months = first === 1 && last === 12;

  function toggle(number: number) {
    onChange(picked.includes(number) ? picked.filter((item) => item !== number) : [...picked, number]);
  }

  return (
    <fieldset
      className="m-0 border-0 p-0 sm:col-span-2"
      aria-invalid={error ? true : undefined}
      aria-describedby={error ? errorId : undefined}
    >
      <legend className="mb-1 p-0 font-semibold">{label}</legend>
      <div className="flex flex-wrap gap-2">
        {numbers.map((number) => (
          <label
            key={number}
            className="flex min-h-11 cursor-pointer items-center gap-2 rounded-control border border-stitch px-3 has-checked:border-accent has-checked:bg-soft"
          >
            <input type="checkbox" checked={picked.includes(number)} onChange={() => toggle(number)} />
            {months ? MONTHS[number - 1] : number}
          </label>
        ))}
      </div>
      {error && (
        <span id={errorId} className="text-sm text-problem">
          {error}
        </span>
      )}
    </fieldset>
  );
}

type LabelledProps = {
  label: string;
  hint?: string;
  error?: string;
  wide?: boolean;
  children: (id: string, describedBy: string | undefined) => ReactNode;
};

function Labelled({ label, hint, error, wide = false, children }: LabelledProps) {
  const id = useId();
  const messageId = `${id}-message`;
  const message = error ?? hint;
  return (
    <div className={`flex flex-col gap-1 ${wide ? "sm:col-span-2" : ""}`}>
      <label htmlFor={id} className="font-semibold">
        {label}
      </label>
      {children(id, message ? messageId : undefined)}
      {message && (
        <span id={messageId} className={`text-sm ${error ? "text-problem" : "text-muted"}`}>
          {message}
        </span>
      )}
    </div>
  );
}

function inputClass(error?: string): string {
  return `min-h-12 w-full rounded-control border bg-surface px-3.5 text-ink ${error ? "border-problem" : "border-stitch"}`;
}
