import {
    useEffect,
    useLayoutEffect,
    useState,
    type CSSProperties,
} from "react";
import { subwayApi } from "../../apis/subwayApi";
import { ApiError } from "../../types/api";

export interface Station {
    id: number;
    name: string;
    region: string;
}

interface StationSelectModalProps {
    title: string;
    onClose: () => void;
    onSelect: (station: Station) => void;
}

export default function StationSelectModal({
    title,
    onClose,
    onSelect,
}: StationSelectModalProps) {
    const [query, setQuery] = useState("");
    const [stations, setStations] = useState<Station[]>([]);
    const [isLoading, setIsLoading] = useState(false);
    const [errorMessage, setErrorMessage] = useState("");
    const [viewportStyle, setViewportStyle] = useState<CSSProperties>();

    useLayoutEffect(() => {
        const root = document.getElementById("root");
        const previousBodyHeight = document.body.style.height;
        const previousRootHeight = root?.style.height ?? "";
        const pageHeight = document.body.getBoundingClientRect().height;

        // iOS에서 키보드가 열릴 때 뒤 페이지의 하단 고정 요소가 따라 움직이지 않게 한다.
        document.body.style.height = `${pageHeight}px`;
        if (root) {
            root.style.height = `${pageHeight}px`;
        }

        return () => {
            document.body.style.height = previousBodyHeight;
            if (root) {
                root.style.height = previousRootHeight;
            }
        };
    }, []);

    useLayoutEffect(() => {
        const viewport = window.visualViewport;

        if (!viewport) {
            return;
        }

        const viewportBottom = viewport.offsetTop + viewport.height;

        const syncWithVisualViewport = () => {
            const visibleHeight = viewport.height;

            setViewportStyle({
                top: `${Math.max(0, viewportBottom - visibleHeight)}px`,
                bottom: "auto",
                height: `${visibleHeight + 1}px`,
            });
        };

        syncWithVisualViewport();
        viewport.addEventListener("resize", syncWithVisualViewport);

        return () => {
            viewport.removeEventListener("resize", syncWithVisualViewport);
        };
    }, []);

    useEffect(() => {
        const handleKeyDown = (event: KeyboardEvent) => {
            if (event.key === "Escape") {
                onClose();
            }
        };

        window.addEventListener("keydown", handleKeyDown);
        return () => window.removeEventListener("keydown", handleKeyDown);
    }, [onClose]);

    useEffect(() => {
        const keyword = query.trim();
        if (!keyword) {
            setStations([]);
            setErrorMessage("");
            setIsLoading(false);
            return;
        }

        if (!/[가-힣0-9]/.test(keyword)) {
            setStations([]);
            setErrorMessage("검색어는 한글과 숫자만 입력해주세요.");
            setIsLoading(false);
            return;
        }

        let canceled = false;
        setStations([]);
        setErrorMessage("");
        setIsLoading(true);

        const timer = window.setTimeout(async () => {
            try {
                const result = await subwayApi.search(keyword);
                if (!canceled) {
                    setStations(
                        result.map((station) => ({
                            id: station.id,
                            name: station.stationName,
                            region: station.routeName,
                        })),
                    );
                }
            } catch (error) {
                if (!canceled) {
                    setStations([]);
                    setErrorMessage(
                        error instanceof ApiError
                            ? error.message
                            : "지하철역 검색 중 오류가 발생했습니다.",
                    );
                }
            } finally {
                if (!canceled) {
                    setIsLoading(false);
                }
            }
        }, 300);

        return () => {
            canceled = true;
            window.clearTimeout(timer);
        };
    }, [query]);

    return (
        <div
            className="bottom-sheet-backdrop-in fixed inset-0 z-[70] mx-auto w-full max-w-[402px] bg-black/40"
            onClick={onClose}
        >
            <div
                className="pointer-events-none absolute inset-x-0 flex items-end"
                style={viewportStyle}
            >
                <section
                    className="bottom-sheet-panel-in pointer-events-auto flex max-h-[calc(100%-12px)] w-full flex-col overflow-hidden rounded-t-[30px] bg-white pb-[max(1.5rem,env(safe-area-inset-bottom))] pt-3 shadow-2xl"
                    role="dialog"
                    aria-modal="true"
                    aria-labelledby="station-modal-title"
                    onClick={(event) => event.stopPropagation()}
                >
                    <div
                        className="mx-auto h-1 w-[55px] rounded-full bg-gray-200"
                        aria-hidden="true"
                    />

                    <div className="mx-5 mt-6 flex items-center justify-between">
                        <h2
                            id="station-modal-title"
                            className="text-xl font-bold text-gray-700"
                        >
                            {title}
                        </h2>
                        <button
                            type="button"
                            className="flex h-9 w-9 items-center justify-center rounded-full text-2xl text-gray-500 hover:bg-gray-100"
                            onClick={onClose}
                            aria-label="역 선택 닫기"
                        >
                            ×
                        </button>
                    </div>

                    <label className="mx-5 mt-5 block">
                        <span className="sr-only">역 검색</span>
                        <input
                            type="search"
                            value={query}
                            onChange={(event) => setQuery(event.target.value)}
                            placeholder="역 이름 또는 지역을 검색해주세요"
                            className="w-full rounded-xl bg-gray-50 px-4 py-3.5 text-[15px] text-gray-700 outline-none placeholder:text-gray-400"
                            enterKeyHint="search"
                        />
                    </label>

                    <div className="scrollbar-hidden mt-4 min-h-0 overflow-x-hidden overflow-y-auto">
                        {isLoading ? (
                            <p className="px-5 py-12 text-center text-sm text-gray-500">
                                검색중입니다...
                            </p>
                        ) : errorMessage ? (
                            <p className="px-5 py-12 text-center text-sm text-red-500">
                                {errorMessage}
                            </p>
                        ) : stations.length > 0 ? (
                            <ul className="divide-y divide-gray-100">
                                {stations.map((station) => (
                                    <li key={station.id}>
                                        <button
                                            type="button"
                                            className="flex w-full items-center justify-between px-5 py-4 text-left transition-colors hover:bg-gray-50 active:bg-gray-100"
                                            onClick={() => onSelect(station)}
                                        >
                                            <span className="font-semibold text-gray-700">
                                                {station.name}
                                            </span>
                                            <span className="text-sm text-gray-500">
                                                {station.region}
                                            </span>
                                        </button>
                                    </li>
                                ))}
                            </ul>
                        ) : (
                            <p className="px-5 py-12 text-center text-sm text-gray-500">
                                {query.trim()
                                    ? "검색 결과가 없습니다."
                                    : "역 이름을 검색해주세요."}
                            </p>
                        )}
                    </div>
                </section>
            </div>
        </div>
    );
}
