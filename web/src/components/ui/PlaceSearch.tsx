"use client";

import { useId, useState, type KeyboardEvent } from "react";

export type PlaceOption = {
  id: string;
  name: string;
  /** The same place in the other language, shown beside the name. */
  otherName?: string;
};

type PlaceSearchProps = {
  label: string;
  options: readonly PlaceOption[];
  query: string;
  onQueryChange: (query: string) => void;
  onSelect: (option: PlaceOption) => void;
  noResultsText: string;
  error?: string;
};

/**
 * An editable combobox following the WAI-ARIA pattern. The parent owns the
 * search, so this same control works with local data and the API.
 */
export function PlaceSearch({
  label,
  options,
  query,
  onQueryChange,
  onSelect,
  noResultsText,
  error,
}: PlaceSearchProps) {
  const id = useId();
  const listId = `${id}-list`;
  const errorId = `${id}-error`;
  const [open, setOpen] = useState(false);
  const [activeIndex, setActiveIndex] = useState(0);

  const hasQuery = query.trim().length > 0;
  const showList = open && hasQuery;
  const activeOption = showList ? options[activeIndex] : undefined;

  function select(option: PlaceOption) {
    onSelect(option);
    setOpen(false);
  }

  function moveActive(step: number) {
    if (options.length > 0) {
      setOpen(true);
      setActiveIndex((index) => (index + step + options.length) % options.length);
    }
  }

  function handleKeyDown(event: KeyboardEvent<HTMLInputElement>) {
    if (event.key === "ArrowDown") {
      event.preventDefault();
      moveActive(1);
    } else if (event.key === "ArrowUp") {
      event.preventDefault();
      moveActive(-1);
    } else if (event.key === "Enter" && activeOption) {
      event.preventDefault();
      select(activeOption);
    } else if (event.key === "Escape") {
      setOpen(false);
    }
  }

  return (
    <div className="flex flex-col gap-1">
      <label htmlFor={id} className="font-semibold">
        {label}
      </label>
      <input
        id={id}
        role="combobox"
        aria-expanded={showList}
        aria-controls={listId}
        aria-autocomplete="list"
        aria-activedescendant={activeOption ? `${listId}-${activeOption.id}` : undefined}
        aria-invalid={error ? true : undefined}
        aria-describedby={error ? errorId : undefined}
        autoComplete="off"
        value={query}
        onChange={(event) => {
          onQueryChange(event.target.value);
          setActiveIndex(0);
          setOpen(true);
        }}
        onKeyDown={handleKeyDown}
        onBlur={() => setOpen(false)}
        className={`min-h-12 w-full rounded-control border bg-surface px-3.5 text-ink ${
          error ? "border-problem" : "border-stitch"
        }`}
      />
      {error && (
        <span id={errorId} className="text-sm text-problem">
          {error}
        </span>
      )}
      <ul
        id={listId}
        role="listbox"
        aria-label={label}
        hidden={!showList}
        className="mt-1 flex list-none flex-col rounded-card border border-line bg-surface p-1"
      >
        {options.map((option, index) => (
          <li
            key={option.id}
            id={`${listId}-${option.id}`}
            role="option"
            aria-selected={index === activeIndex}
            // mousedown, not click: a click would blur the input and close the list first.
            onMouseDown={(event) => {
              event.preventDefault();
              select(option);
            }}
            className={`flex min-h-11 cursor-pointer items-center justify-between gap-3 rounded-control px-3 ${
              index === activeIndex ? "bg-soft font-semibold text-soft-ink" : ""
            }`}
          >
            <span>{option.name}</span>
            {option.otherName && <span className="font-normal">{option.otherName}</span>}
          </li>
        ))}
        {options.length === 0 && <li className="px-3 py-2 text-muted">{noResultsText}</li>}
      </ul>
    </div>
  );
}
