export type RouteStop = {
  name: string;
  /** How to reach the next stop, such as "Train, about 4 h 30 m". */
  legToNext?: string;
};

/** The running stitch from the design: the site's one decorative idea. */
export function RouteLine({ stops }: { stops: readonly RouteStop[] }) {
  return (
    <ol className="m-0 flex list-none flex-col p-0">
      {stops.map((stop, index) => (
        <li key={`${stop.name}-${index}`}>
          <div className="flex items-center gap-3.5">
            <span
              aria-hidden="true"
              className={`size-3 rounded-full border-2 border-accent ${
                index === 0 ? "bg-accent" : "bg-transparent"
              }`}
            />
            <span className="font-semibold">{stop.name}</span>
          </div>
          {stop.legToNext && index < stops.length - 1 && (
            <p className="m-0 ml-[5px] border-l-2 border-dashed border-stitch py-2.5 pl-[19px] text-sm text-muted">
              {stop.legToNext}
            </p>
          )}
        </li>
      ))}
    </ol>
  );
}
