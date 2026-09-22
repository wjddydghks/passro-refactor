import coordinateDataset from "../data/subwayStationCoordinates.json";
import type { SubwayStationItem } from "../apis/subwayApi";

export interface GeographicCoordinates {
    latitude: number;
    longitude: number;
}

export interface SubwayStationCoordinate extends GeographicCoordinates {
    stationCode: string;
    stationName: string;
    routeName: string;
    region: string | null;
}

export interface NearestRouteStation {
    station: SubwayStationItem;
    stationIndex: number;
    coordinate: SubwayStationCoordinate;
    distanceMeters: number;
}

export interface ResolvedRouteStation {
    station: SubwayStationItem;
    stationIndex: number;
    coordinate: SubwayStationCoordinate | null;
}

interface CoordinateDataset {
    sources: Array<{
        name: string;
        version: string;
        url: string;
    }>;
    stations: SubwayStationCoordinate[];
}

const dataset = coordinateDataset as CoordinateDataset;
const EARTH_RADIUS_METERS = 6_371_000;

const STATION_ALIASES: Record<string, string> = {
    신길온천: "능길",
    운동장송담대: "용인중앙시장",
};

function compactStationName(name: string, removeParenthetical: boolean) {
    const normalized = name
        .normalize("NFKC")
        .trim()
        .replace(/[·ㆍ.]/g, "")
        .replace(/\s+/g, "");
    const withoutParenthetical = removeParenthetical
        ? normalized.replace(/[（(].*$/u, "")
        : normalized;

    return withoutParenthetical.replace(/역$/u, "");
}

function normalizeRouteName(routeName: string, region?: string | null) {
    const compact = routeName.normalize("NFKC").replace(/\s+/g, "");

    if (
        region === "수도권" &&
        ["경부선", "경원선", "경인선", "장항선"].includes(compact)
    ) {
        return "1호선";
    }

    if (region === "수도권" && compact === "일산선") {
        return "3호선";
    }

    if (
        region === "수도권" &&
        ["안산과천선", "진접선"].includes(compact)
    ) {
        return "4호선";
    }

    const aliases: Record<string, string> = {
        경강선: "경강",
        경의중앙선: "경의중앙",
        경춘선: "경춘",
        김포도시철도: "김포골드라인",
        동해선: "동해",
        분당선: "수인분당",
        수인선: "수인분당",
        수인분당선: "수인분당",
        신분당선: "신분당",
        인천국제공항선: "공항",
        인천지하철1호선: "인천1호선",
        인천지하철2호선: "인천2호선",
        자기부상철도: "자기부상",
    };

    if (aliases[compact]) {
        return aliases[compact];
    }

    return compact
        .replace(/^수도권광역철도/u, "")
        .replace(/^수도권도시철도/u, "")
        .replace(/^서울도시철도/u, "")
        .replace(/^부산도시철도/u, "")
        .replace(/^부산경량도시철도/u, "")
        .replace(/^대구도시철도/u, "")
        .replace(/^대전도시철도/u, "")
        .replace(/^광주도시철도/u, "")
        .replace(/^도시철도/u, "")
        .replace(/^수도권경량도시철도/u, "")
        .replace(/선$/u, "");
}

function createCoordinateIndex(removeParenthetical: boolean) {
    const index = new Map<string, SubwayStationCoordinate[]>();

    for (const station of dataset.stations) {
        const key = compactStationName(
            station.stationName,
            removeParenthetical,
        );
        const stations = index.get(key) ?? [];
        stations.push(station);
        index.set(key, stations);
    }

    return index;
}

const exactCoordinateIndex = createCoordinateIndex(false);
const baseNameCoordinateIndex = createCoordinateIndex(true);

function getCoordinateCandidates(station: SubwayStationItem) {
    const exactName = compactStationName(station.stationName, false);
    const baseName = compactStationName(station.stationName, true);
    const aliasName = STATION_ALIASES[baseName];
    const candidates =
        exactCoordinateIndex.get(exactName) ??
        baseNameCoordinateIndex.get(baseName) ??
        (aliasName ? baseNameCoordinateIndex.get(aliasName) : undefined) ??
        [];

    const sameRegion = station.region
        ? candidates.filter((candidate) => candidate.region === station.region)
        : candidates;
    const regionCandidates = sameRegion.length > 0 ? sameRegion : candidates;
    const normalizedRouteName = normalizeRouteName(
        station.routeName,
        station.region,
    );
    const sameRoute = regionCandidates.filter(
        (candidate) =>
            normalizeRouteName(candidate.routeName, candidate.region) ===
            normalizedRouteName,
    );

    return sameRoute.length > 0 ? sameRoute : regionCandidates;
}

function toRadians(value: number) {
    return (value * Math.PI) / 180;
}

export function calculateDistanceMeters(
    from: GeographicCoordinates,
    to: GeographicCoordinates,
) {
    const latitudeDelta = toRadians(to.latitude - from.latitude);
    const longitudeDelta = toRadians(to.longitude - from.longitude);
    const fromLatitude = toRadians(from.latitude);
    const toLatitude = toRadians(to.latitude);
    const haversine =
        Math.sin(latitudeDelta / 2) ** 2 +
        Math.cos(fromLatitude) *
            Math.cos(toLatitude) *
            Math.sin(longitudeDelta / 2) ** 2;

    return (
        2 *
        EARTH_RADIUS_METERS *
        Math.atan2(Math.sqrt(haversine), Math.sqrt(1 - haversine))
    );
}

export function findNearestRouteStation(
    position: GeographicCoordinates,
    routeStations: SubwayStationItem[],
): NearestRouteStation | null {
    let nearest: NearestRouteStation | null = null;

    routeStations.forEach((station, stationIndex) => {
        for (const coordinate of getCoordinateCandidates(station)) {
            const distanceMeters = calculateDistanceMeters(
                position,
                coordinate,
            );

            if (!nearest || distanceMeters < nearest.distanceMeters) {
                nearest = {
                    station,
                    stationIndex,
                    coordinate,
                    distanceMeters,
                };
            }
        }
    });

    return nearest;
}

export function getRouteCoordinateCoverage(
    routeStations: SubwayStationItem[],
) {
    return routeStations.reduce(
        (count, station) =>
            count + (getCoordinateCandidates(station).length > 0 ? 1 : 0),
        0,
    );
}

export function resolveRouteStationCoordinates(
    routeStations: SubwayStationItem[],
): ResolvedRouteStation[] {
    return routeStations.map((station, stationIndex) => ({
        station,
        stationIndex,
        coordinate: getCoordinateCandidates(station)[0] ?? null,
    }));
}

export const subwayCoordinateSources = dataset.sources;
