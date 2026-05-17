import type { NotmidPlace } from "@notmid/contracts";
import { notmidRoutes } from "@notmid/contracts";
import Link from "next/link";

type MapBoardProps = {
  places: NotmidPlace[];
  compact?: boolean;
};

export function MapBoard({ places, compact = false }: MapBoardProps) {
  return (
    <div className={compact ? "map-board compact" : "map-board"}>
      <div className="panel-heading">
        <span>Map</span>
        <strong>{places.length}</strong>
      </div>
      <div className="map-grid" aria-label="place map preview">
        {places.map((place, index) => (
          <Link
            className={`map-pin pin-${index + 1}`}
            href={notmidRoutes.place(place.id)}
            key={place.id}
            style={{
              backgroundImage: `linear-gradient(180deg, rgba(8, 9, 7, 0.02), rgba(8, 9, 7, 0.68)), url(${place.coverImageUrl})`,
            }}
          >
            <span>{place.name}</span>
            <small>{place.score}</small>
          </Link>
        ))}
      </div>
    </div>
  );
}
