"use client";

import { useId } from "react";

type Choice<T extends string> = { value: T; label: string };

type ChoiceGroupProps<T extends string> = {
  label: string;
  choices: readonly Choice<T>[];
  value: T;
  onChange: (value: T) => void;
};

/** Native radios, so arrow keys and screen readers work without extra code. */
export function ChoiceGroup<T extends string>({
  label,
  choices,
  value,
  onChange,
}: ChoiceGroupProps<T>) {
  const name = useId();

  return (
    <fieldset className="m-0 min-w-0 border-0 p-0">
      <legend className="mb-1 p-0 font-semibold">{label}</legend>
      <div className="flex gap-1 rounded-card bg-sunk p-1">
        {choices.map((choice) => (
          <label key={choice.value} className="flex-1 cursor-pointer">
            <input
              type="radio"
              name={name}
              value={choice.value}
              checked={choice.value === value}
              onChange={() => onChange(choice.value)}
              className="peer sr-only"
            />
            <span className="flex min-h-11 items-center justify-center rounded-control border border-transparent px-2 text-center font-medium text-muted peer-checked:border-line peer-checked:bg-surface peer-checked:font-semibold peer-checked:text-ink peer-focus-visible:outline-2 peer-focus-visible:outline-offset-2 peer-focus-visible:outline-accent">
              {choice.label}
            </span>
          </label>
        ))}
      </div>
    </fieldset>
  );
}
