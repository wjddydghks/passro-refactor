import {
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
  type PointerEvent as ReactPointerEvent,
} from "react";
import type { SubwayStationItem } from "../../apis/subwayApi";
import {
  resolveRouteStationCoordinates,
  type GeographicCoordinates,
} from "../../utils/subwayLocation";

interface GeographicRouteMapProps {
  stations: SubwayStationItem[];
  currentPlaceId?: number;
  currentCoordinates?: GeographicCoordinates;
  compact?: boolean;
}

interface MapView {
  center: GeographicCoordinates;
  zoom: number;
}

interface PixelPoint {
  x: number;
  y: number;
}

interface VisibleTile extends PixelPoint {
  key: string;
  url: string;
}

interface StationLabelPlacement {
  stationIndex: number;
  x: number;
  y: number;
  width: number;
  height: number;
  label: string;
}

const TILE_SIZE = 256;
const TILE_OVERSCAN = 1;
const MIN_ZOOM = 5;
const MAX_ZOOM = 18;
const DEFAULT_CENTER = { latitude: 37.5665, longitude: 126.978 };
const DEFAULT_TILE_TEMPLATE = "https://tile.openstreetmap.org/{z}/{x}/{y}.png";
const TILE_TEMPLATE =
  import.meta.env.VITE_MAP_TILE_URL?.trim() || DEFAULT_TILE_TEMPLATE;

const LINE_COLORS: Record<string, string> = {
  "1호선": "#2563EB",
  "2호선": "#16A34A",
  "3호선": "#D97706",
  "4호선": "#0EA5E9",
  "5호선": "#7C3AED",
  "6호선": "#9A3412",
  "7호선": "#64748B",
  "8호선": "#DB2777",
  "9호선": "#B59B17",
  "GTX-A": "#7C2D12",
  경강: "#2563EB",
  경의중앙: "#0F766E",
  경춘: "#22C55E",
  공항: "#0891B2",
  김포골드라인: "#B45309",
  동해: "#0EA5E9",
  부산김해경전철: "#7C3AED",
  서해선: "#84CC16",
  수인분당: "#EAB308",
  신림선: "#2563EB",
  신분당: "#DC2626",
  에버라인: "#65A30D",
  우이신설: "#94A3B8",
  의정부: "#F97316",
  인천1호선: "#38BDF8",
  인천2호선: "#F97316",
};

export function getSubwayLineColor(routeName: string) {
  return LINE_COLORS[routeName] ?? "#7C3AED";
}

function clamp(value: number, minimum: number, maximum: number) {
  return Math.min(maximum, Math.max(minimum, value));
}

function getSchematicSegmentPath(start: PixelPoint, end: PixelPoint) {
  const deltaX = end.x - start.x;
  const deltaY = end.y - start.y;
  const absoluteX = Math.abs(deltaX);
  const absoluteY = Math.abs(deltaY);

  if (absoluteX >= absoluteY) {
    const bendX = end.x - Math.sign(deltaX) * absoluteY;
    return `M ${start.x} ${start.y} H ${bendX} L ${end.x} ${end.y}`;
  }

  const bendY = end.y - Math.sign(deltaY) * absoluteX;
  return `M ${start.x} ${start.y} V ${bendY} L ${end.x} ${end.y}`;
}

function coordinatesToWorld(
  coordinates: GeographicCoordinates,
  zoom: number,
): PixelPoint {
  const worldSize = TILE_SIZE * 2 ** zoom;
  const latitude = clamp(coordinates.latitude, -85.05112878, 85.05112878);
  const sinLatitude = Math.sin((latitude * Math.PI) / 180);

  return {
    x: ((coordinates.longitude + 180) / 360) * worldSize,
    y:
      (0.5 - Math.log((1 + sinLatitude) / (1 - sinLatitude)) / (4 * Math.PI)) *
      worldSize,
  };
}

function worldToCoordinates(point: PixelPoint, zoom: number) {
  const worldSize = TILE_SIZE * 2 ** zoom;
  const mercatorY = Math.PI - (2 * Math.PI * point.y) / worldSize;

  return {
    latitude: (Math.atan(Math.sinh(mercatorY)) * 180) / Math.PI,
    longitude: (point.x / worldSize) * 360 - 180,
  };
}

function coordinatesToScreen(
  coordinates: GeographicCoordinates,
  view: MapView,
  width: number,
  height: number,
) {
  const center = coordinatesToWorld(view.center, view.zoom);
  const point = coordinatesToWorld(coordinates, view.zoom);

  return {
    x: point.x - center.x + width / 2,
    y: point.y - center.y + height / 2,
  };
}

function fitCoordinates(
  coordinates: GeographicCoordinates[],
  width: number,
  height: number,
): MapView {
  if (coordinates.length === 0 || width <= 0 || height <= 0) {
    return { center: DEFAULT_CENTER, zoom: 12 };
  }

  if (coordinates.length === 1) {
    return { center: coordinates[0], zoom: 15 };
  }

  const padding = 52;

  for (let zoom = MAX_ZOOM; zoom >= MIN_ZOOM; zoom -= 1) {
    const points = coordinates.map((coordinate) =>
      coordinatesToWorld(coordinate, zoom),
    );
    const xValues = points.map((point) => point.x);
    const yValues = points.map((point) => point.y);
    const minimumX = Math.min(...xValues);
    const maximumX = Math.max(...xValues);
    const minimumY = Math.min(...yValues);
    const maximumY = Math.max(...yValues);

    if (
      maximumX - minimumX <= Math.max(1, width - padding * 2) &&
      maximumY - minimumY <= Math.max(1, height - padding * 2)
    ) {
      return {
        center: worldToCoordinates(
          {
            x: (minimumX + maximumX) / 2,
            y: (minimumY + maximumY) / 2,
          },
          zoom,
        ),
        zoom,
      };
    }
  }

  return {
    center: coordinates[0],
    zoom: MIN_ZOOM,
  };
}

function createVisibleTiles(
  view: MapView,
  width: number,
  height: number,
): VisibleTile[] {
  if (width <= 0 || height <= 0) {
    return [];
  }

  const center = coordinatesToWorld(view.center, view.zoom);
  const firstTileX =
    Math.floor((center.x - width / 2) / TILE_SIZE) - TILE_OVERSCAN;
  const lastTileX =
    Math.floor((center.x + width / 2) / TILE_SIZE) + TILE_OVERSCAN;
  const firstTileY =
    Math.floor((center.y - height / 2) / TILE_SIZE) - TILE_OVERSCAN;
  const lastTileY =
    Math.floor((center.y + height / 2) / TILE_SIZE) + TILE_OVERSCAN;
  const tileCount = 2 ** view.zoom;
  const tiles: VisibleTile[] = [];

  for (let tileY = firstTileY; tileY <= lastTileY; tileY += 1) {
    if (tileY < 0 || tileY >= tileCount) {
      continue;
    }

    for (let tileX = firstTileX; tileX <= lastTileX; tileX += 1) {
      const wrappedTileX = ((tileX % tileCount) + tileCount) % tileCount;
      const url = TILE_TEMPLATE.replace("{z}", String(view.zoom))
        .replace("{x}", String(wrappedTileX))
        .replace("{y}", String(tileY));

      tiles.push({
        key: `${view.zoom}-${tileX}-${tileY}`,
        url,
        x: tileX * TILE_SIZE - center.x + width / 2,
        y: tileY * TILE_SIZE - center.y + height / 2,
      });
    }
  }

  return tiles;
}

function getStationLabel(stationName: string) {
  return stationName.replace(/[（(].*$/u, "");
}

function createStationLabelPlacements(
  screenStations: Array<{
    station: SubwayStationItem;
    point: PixelPoint | null;
  }>,
  currentStationIndex: number,
  width: number,
  height: number,
) {
  const candidates = screenStations
    .map(({ station, point }, stationIndex) => {
      const isEndpoint =
        stationIndex === 0 || stationIndex === screenStations.length - 1;
      const isCurrent = stationIndex === currentStationIndex;

      return {
        station,
        stationIndex,
        point,
        shouldShow: isEndpoint || isCurrent,
        priority: isEndpoint ? 0 : 1,
      };
    })
    .filter((candidate) => candidate.point && candidate.shouldShow)
    .sort(
      (left, right) =>
        left.priority - right.priority ||
        left.stationIndex - right.stationIndex,
    );
  const placements: StationLabelPlacement[] = [];

  for (const candidate of candidates) {
    const point = candidate.point;
    if (!point) {
      continue;
    }

    const previousPoint =
      screenStations[candidate.stationIndex - 1]?.point ?? point;
    const nextPoint =
      screenStations[candidate.stationIndex + 1]?.point ?? point;
    const deltaX = nextPoint.x - previousPoint.x;
    const deltaY = nextPoint.y - previousPoint.y;
    const length = Math.hypot(deltaX, deltaY) || 1;
    const normalX = -deltaY / length;
    const normalY = deltaX / length;
    const tangentX = deltaX / length;
    const tangentY = deltaY / length;
    const preferredSide = candidate.stationIndex % 2 === 0 ? 1 : -1;
    const label = getStationLabel(candidate.station.stationName).slice(0, 9);
    const labelWidth = clamp(label.length * 9 + 16, 44, 88);
    const labelHeight = 20;
    const normalOffsets =
      candidate.priority <= 1 ? [29, 43, 58, 74] : [25, 39, 54, 70];
    const tangentOffsets = [0, -18, 18, -36, 36];
    const placementCandidates: StationLabelPlacement[] = [];

    for (const side of [preferredSide, -preferredSide]) {
      for (const normalOffset of normalOffsets) {
        for (const tangentOffset of tangentOffsets) {
          placementCandidates.push({
            stationIndex: candidate.stationIndex,
            x: clamp(
              point.x +
                normalX * normalOffset * side +
                tangentX * tangentOffset,
              labelWidth / 2 + 6,
              Math.max(labelWidth / 2 + 6, width - labelWidth / 2 - 6),
            ),
            y: clamp(
              point.y +
                normalY * normalOffset * side +
                tangentY * tangentOffset,
              labelHeight / 2 + 6,
              Math.max(labelHeight / 2 + 6, height - labelHeight / 2 - 30),
            ),
            width: labelWidth,
            height: labelHeight,
            label,
          });
        }
      }
    }

    const overlapScore = (nextPlacement: StationLabelPlacement) =>
      placements.reduce((score, placement) => {
        const overlapWidth = Math.max(
          0,
          (placement.width + nextPlacement.width) / 2 +
            5 -
            Math.abs(placement.x - nextPlacement.x),
        );
        const overlapHeight = Math.max(
          0,
          (placement.height + nextPlacement.height) / 2 +
            5 -
            Math.abs(placement.y - nextPlacement.y),
        );

        return score + overlapWidth * overlapHeight;
      }, 0);
    const nextPlacement = placementCandidates.reduce(
      (bestPlacement, placement) =>
        overlapScore(placement) < overlapScore(bestPlacement)
          ? placement
          : bestPlacement,
      placementCandidates[0],
    );

    placements.push(nextPlacement);
  }

  return placements;
}

export function GeographicRouteMap({
  stations,
  currentPlaceId,
  currentCoordinates,
  compact = false,
}: GeographicRouteMapProps) {
  const containerRef = useRef<HTMLDivElement>(null);
  const dragRef = useRef<{
    pointerId: number;
    startX: number;
    startY: number;
    center: PixelPoint;
  } | null>(null);
  const [size, setSize] = useState({ width: 0, height: 0 });
  const [view, setView] = useState<MapView>({
    center: DEFAULT_CENTER,
    zoom: 12,
  });
  const [hoveredStationIndex, setHoveredStationIndex] = useState<number | null>(
    null,
  );
  const resolvedStations = useMemo(
    () => resolveRouteStationCoordinates(stations),
    [stations],
  );
  const routeCoordinates = useMemo(
    () =>
      resolvedStations.flatMap(({ coordinate }) =>
        coordinate ? [coordinate] : [],
      ),
    [resolvedStations],
  );
  const fitTargets =
    routeCoordinates.length > 0
      ? routeCoordinates
      : currentCoordinates
        ? [currentCoordinates]
        : [];
  const fitSignature = useMemo(
    () =>
      resolvedStations
        .map(({ station, coordinate }) =>
          coordinate
            ? `${station.id}:${coordinate.latitude}:${coordinate.longitude}`
            : `${station.id}:missing`,
        )
        .join("|"),
    [resolvedStations],
  );

  useEffect(() => {
    const container = containerRef.current;
    if (!container) {
      return;
    }

    const updateSize = () => {
      setSize({
        width: container.clientWidth,
        height: container.clientHeight,
      });
    };
    const observer = new ResizeObserver(updateSize);

    updateSize();
    observer.observe(container);

    return () => observer.disconnect();
  }, []);

  useEffect(() => {
    if (size.width === 0 || size.height === 0) {
      return;
    }

    setView(fitCoordinates(fitTargets, size.width, size.height));
  }, [fitSignature, size.width, size.height]);

  const currentStationIndex = stations.findIndex(
    (station) => station.id === currentPlaceId,
  );
  const currentStationCoordinate =
    currentStationIndex >= 0
      ? resolvedStations[currentStationIndex]?.coordinate
      : null;
  const markerCoordinates = currentCoordinates ?? currentStationCoordinate;
  const visibleTiles = useMemo(
    () => createVisibleTiles(view, size.width, size.height),
    [size.height, size.width, view],
  );
  const screenStations = useMemo(
    () =>
      resolvedStations.map((resolvedStation) => ({
        ...resolvedStation,
        point: resolvedStation.coordinate
          ? coordinatesToScreen(
              resolvedStation.coordinate,
              view,
              size.width,
              size.height,
            )
          : null,
      })),
    [resolvedStations, size.height, size.width, view],
  );
  const currentMarkerPoint = markerCoordinates
    ? coordinatesToScreen(markerCoordinates, view, size.width, size.height)
    : null;
  const stationLabelPlacements = useMemo(
    () =>
      createStationLabelPlacements(
        screenStations,
        currentStationIndex,
        size.width,
        size.height,
      ),
    [currentStationIndex, screenStations, size.height, size.width],
  );

  const resetToRoute = useCallback(() => {
    setView(fitCoordinates(fitTargets, size.width, size.height));
  }, [fitTargets, size.height, size.width]);

  const centerCurrentLocation = useCallback(() => {
    if (!markerCoordinates) {
      return;
    }

    setView((currentView) => ({
      center: markerCoordinates,
      zoom: Math.max(currentView.zoom, 15),
    }));
  }, [markerCoordinates]);

  const changeZoom = useCallback((delta: number) => {
    setView((currentView) => ({
      ...currentView,
      zoom: clamp(currentView.zoom + delta, MIN_ZOOM, MAX_ZOOM),
    }));
  }, []);

  const handlePointerDown = (event: ReactPointerEvent<HTMLDivElement>) => {
    if (!event.isPrimary || event.button !== 0) {
      return;
    }

    event.currentTarget.setPointerCapture(event.pointerId);
    dragRef.current = {
      pointerId: event.pointerId,
      startX: event.clientX,
      startY: event.clientY,
      center: coordinatesToWorld(view.center, view.zoom),
    };
  };

  const handlePointerMove = (event: ReactPointerEvent<HTMLDivElement>) => {
    const drag = dragRef.current;
    if (!drag || drag.pointerId !== event.pointerId) {
      return;
    }

    const worldSize = TILE_SIZE * 2 ** view.zoom;
    const nextCenter = {
      x: drag.center.x - (event.clientX - drag.startX),
      y: clamp(drag.center.y - (event.clientY - drag.startY), 0, worldSize),
    };

    setView((currentView) => ({
      ...currentView,
      center: worldToCoordinates(nextCenter, currentView.zoom),
    }));
  };

  const handlePointerEnd = (event: ReactPointerEvent<HTMLDivElement>) => {
    if (dragRef.current?.pointerId !== event.pointerId) {
      return;
    }

    dragRef.current = null;
    if (event.currentTarget.hasPointerCapture(event.pointerId)) {
      event.currentTarget.releasePointerCapture(event.pointerId);
    }
  };

  return (
    <div
      ref={containerRef}
      className={`relative w-full cursor-grab touch-none overflow-hidden bg-slate-100 active:cursor-grabbing ${
        compact ? "h-[280px] rounded-[10px]" : "h-[360px]"
      }`}
      onPointerDown={handlePointerDown}
      onPointerMove={handlePointerMove}
      onPointerUp={handlePointerEnd}
      onPointerCancel={handlePointerEnd}
      onDoubleClick={() => changeZoom(1)}
      role="application"
      aria-label={`${stations[0]?.stationName ?? "출발역"}에서 ${stations[stations.length - 1]?.stationName ?? "도착역"}까지의 전달 추적 지도`}
    >
      {visibleTiles.map((tile) => (
        <img
          key={tile.key}
          src={tile.url}
          alt=""
          width={TILE_SIZE}
          height={TILE_SIZE}
          draggable={false}
          decoding="async"
          className="pointer-events-none absolute max-w-none select-none"
          style={{
            left: tile.x,
            top: tile.y,
            width: TILE_SIZE + 1,
            height: TILE_SIZE + 1,
          }}
        />
      ))}

      <svg
        className="pointer-events-none absolute inset-0 h-full w-full"
        viewBox={`0 0 ${Math.max(1, size.width)} ${Math.max(1, size.height)}`}
        aria-hidden="true"
      >
        {screenStations.slice(0, -1).map((current, index) => {
          const next = screenStations[index + 1];
          if (!current.point || !next.point) {
            return null;
          }

          const segmentColor = getSubwayLineColor(current.station.routeName);
          const isPassed =
            currentStationIndex >= 0 && index < currentStationIndex;
          const segmentOpacity =
            currentStationIndex < 0 || isPassed ? 0.95 : 0.62;
          const segmentPath = getSchematicSegmentPath(
            current.point,
            next.point,
          );

          return (
            <g key={`segment-${current.station.id}-${index}`}>
              <path
                d={segmentPath}
                stroke="white"
                strokeWidth="10"
                strokeLinecap="round"
                strokeLinejoin="round"
                opacity="0.92"
                fill="none"
              />
              <path
                d={segmentPath}
                stroke={segmentColor}
                strokeWidth="6"
                strokeLinecap="round"
                strokeLinejoin="round"
                opacity={segmentOpacity}
                fill="none"
              />
            </g>
          );
        })}

        {screenStations.map((resolvedStation, index) => {
          const { point, station } = resolvedStation;
          if (!point) {
            return null;
          }

          const color = getSubwayLineColor(station.routeName);
          const isCurrent = index === currentStationIndex;
          const isEndpoint = index === 0 || index === stations.length - 1;
          const isTransfer =
            stations[index - 1]?.routeName !== station.routeName ||
            stations[index + 1]?.routeName !== station.routeName;

          return (
            <g
              key={`station-${station.id}-${index}`}
              className="pointer-events-auto cursor-pointer"
              onMouseEnter={() => setHoveredStationIndex(index)}
              onMouseLeave={() => setHoveredStationIndex(null)}
            >
              <title>{`${station.stationName} ${station.routeName}`}</title>
              <circle cx={point.x} cy={point.y} r="13" fill="transparent" />
              <circle
                cx={point.x}
                cy={point.y}
                r={isEndpoint || isTransfer ? 7 : 5}
                fill={isCurrent ? color : "white"}
                stroke="white"
                strokeWidth="5"
              />
              <circle
                cx={point.x}
                cy={point.y}
                r={isEndpoint || isTransfer ? 7 : 5}
                fill={isCurrent ? color : "white"}
                stroke={color}
                strokeWidth="3"
              />
            </g>
          );
        })}

        {stationLabelPlacements.map((placement) => {
          const stationPoint = screenStations[placement.stationIndex]?.point;
          if (!stationPoint) {
            return null;
          }

          return (
            <g key={`station-label-${placement.stationIndex}`}>
              <line
                x1={stationPoint.x}
                y1={stationPoint.y}
                x2={placement.x}
                y2={placement.y}
                stroke="white"
                strokeWidth="4"
                strokeLinecap="round"
                opacity="0.9"
              />
              <line
                x1={stationPoint.x}
                y1={stationPoint.y}
                x2={placement.x}
                y2={placement.y}
                stroke="#6B7280"
                strokeWidth="1"
                strokeDasharray="2 2"
                opacity="0.65"
              />
              <rect
                x={placement.x - placement.width / 2}
                y={placement.y - placement.height / 2}
                width={placement.width}
                height={placement.height}
                rx="7"
                fill="white"
                stroke="#E5E7EB"
                strokeWidth="1"
                opacity="0.96"
              />
              <text
                x={placement.x}
                y={placement.y + 3.5}
                textAnchor="middle"
                fill="#111827"
                fontSize="9"
                fontWeight="700"
              >
                {placement.label}
              </text>
            </g>
          );
        })}

        {currentMarkerPoint ? (
          <g
            style={{
              transform: `translate(${currentMarkerPoint.x}px, ${currentMarkerPoint.y}px)`,
              transition: "transform 800ms ease-out",
            }}
          >
            <circle
              r="18"
              fill="#7C3AED"
              opacity="0.22"
              className="animate-ping"
            />
            <circle r="11" fill="#7C3AED" stroke="white" strokeWidth="4" />
            <circle r="3" fill="white" />
          </g>
        ) : null}
      </svg>

      {hoveredStationIndex !== null &&
      screenStations[hoveredStationIndex]?.point ? (
        <span
          className="pointer-events-none absolute z-20 -translate-x-1/2 -translate-y-full whitespace-nowrap rounded-md bg-gray-800 px-2 py-1 text-[11px] font-semibold text-white shadow-md"
          style={{
            left: screenStations[hoveredStationIndex].point?.x,
            top: (screenStations[hoveredStationIndex].point?.y ?? 0) - 11,
          }}
        >
          {screenStations[hoveredStationIndex].station.stationName} ·{" "}
          {screenStations[hoveredStationIndex].station.routeName}
        </span>
      ) : null}

      {currentMarkerPoint ? (
        <span
          className="pointer-events-none absolute rounded-full bg-purple-700 px-2.5 py-1 text-[10px] font-bold text-white shadow-md"
          style={{
            left: currentMarkerPoint.x,
            top: currentMarkerPoint.y - 23,
            transform: "translate(-50%, -100%)",
            transition: "left 800ms ease-out, top 800ms ease-out",
          }}
        >
          현재 위치
        </span>
      ) : null}

      <div
        className="absolute right-3 top-3 flex flex-col overflow-hidden rounded-xl bg-white shadow-md"
        onPointerDown={(event) => event.stopPropagation()}
      >
        <button
          type="button"
          onClick={() => changeZoom(1)}
          disabled={view.zoom >= MAX_ZOOM}
          className="h-10 w-10 border-b border-gray-100 text-xl font-bold text-gray-700 disabled:text-gray-300"
          aria-label="지도 확대"
        >
          +
        </button>
        <button
          type="button"
          onClick={() => changeZoom(-1)}
          disabled={view.zoom <= MIN_ZOOM}
          className="h-10 w-10 text-xl font-bold text-gray-700 disabled:text-gray-300"
          aria-label="지도 축소"
        >
          −
        </button>
      </div>

      <div
        className="absolute bottom-7 left-3 flex gap-2"
        onPointerDown={(event) => event.stopPropagation()}
      >
        <button
          type="button"
          onClick={resetToRoute}
          disabled={fitTargets.length === 0}
          className="rounded-lg bg-white px-3 py-2 text-xs font-bold text-gray-700 shadow-md disabled:text-gray-300"
        >
          경로 전체
        </button>
        <button
          type="button"
          onClick={centerCurrentLocation}
          disabled={!markerCoordinates}
          className="rounded-lg bg-purple-700 px-3 py-2 text-xs font-bold text-white shadow-md disabled:bg-gray-300"
        >
          현재 위치
        </button>
      </div>

      <a
        href="https://www.openstreetmap.org/copyright"
        target="_blank"
        rel="noreferrer"
        className="absolute bottom-0 right-0 bg-white/90 px-1.5 py-0.5 text-[9px] font-medium text-gray-600"
        onPointerDown={(event) => event.stopPropagation()}
      >
        © OpenStreetMap contributors
      </a>
    </div>
  );
}
