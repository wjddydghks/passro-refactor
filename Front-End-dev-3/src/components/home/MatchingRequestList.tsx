import { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import { useDebouncedValue } from "../../hooks/useDebouncedValue";
import type { MatchingRequest } from "../../types/home";
import { getCurrentUser } from "../../utils/auth";

type MatchingRequestListProps = {
    requests: MatchingRequest[];
};

const ITEMS_PER_PAGE = 3;

function getFavoriteStorageKey() {
    const accountId = getCurrentUser()?.id ?? "guest";
    return `passro.favoriteMatchingRequests.${accountId}`;
}

function readFavoriteIds() {
    try {
        const storedValue = localStorage.getItem(getFavoriteStorageKey());
        if (!storedValue) {
            return new Set<number>();
        }

        const parsedValue: unknown = JSON.parse(storedValue);
        if (!Array.isArray(parsedValue)) {
            return new Set<number>();
        }

        return new Set(
            parsedValue.filter(
                (value): value is number =>
                    Number.isSafeInteger(value) && value > 0,
            ),
        );
    } catch {
        return new Set<number>();
    }
}

function saveFavoriteIds(favoriteIds: Set<number>) {
    try {
        localStorage.setItem(
            getFavoriteStorageKey(),
            JSON.stringify([...favoriteIds]),
        );
    } catch {
        // 저장소를 사용할 수 없어도 현재 화면의 찜 상태는 유지한다.
    }
}

function SearchIcon() {
    return (
        <svg viewBox="0 0 24 24" className="h-5 w-5" aria-hidden="true">
            <circle
                cx="10.5"
                cy="10.5"
                r="6.5"
                fill="none"
                stroke="currentColor"
                strokeWidth="1.8"
            />
            <path
                d="m15.5 15.5 4 4"
                fill="none"
                stroke="currentColor"
                strokeWidth="1.8"
                strokeLinecap="round"
            />
        </svg>
    );
}

function FavoriteIcon({ selected }: { selected: boolean }) {
    return (
        <svg viewBox="0 0 24 24" className="h-5 w-5" aria-hidden="true">
            <path
                d="m12 3 2.7 5.5 6.1.9-4.4 4.3 1 6.1-5.4-2.9-5.4 2.9 1-6.1L3.2 9.4l6.1-.9L12 3Z"
                fill={selected ? "currentColor" : "none"}
                stroke="currentColor"
                strokeWidth="1.7"
                strokeLinejoin="round"
            />
        </svg>
    );
}

export function MatchingRequestList({ requests }: MatchingRequestListProps) {
    const navigate = useNavigate();
    const [searchTerm, setSearchTerm] = useState("");
    const [currentPage, setCurrentPage] = useState(1);
    const [favoriteIds, setFavoriteIds] = useState(readFavoriteIds);
    const debouncedSearchTerm = useDebouncedValue(searchTerm, 300);
    const normalizedSearchTerm = debouncedSearchTerm.trim().toLowerCase();
    const isSearching = searchTerm.trim() !== debouncedSearchTerm.trim();

    const filteredRequests = useMemo(() => {
        if (!normalizedSearchTerm) {
            return requests;
        }

        return requests.filter((request) =>
            `${request.title} ${request.route}`
                .toLowerCase()
                .includes(normalizedSearchTerm),
        );
    }, [normalizedSearchTerm, requests]);

    const totalPages = Math.max(
        1,
        Math.ceil(filteredRequests.length / ITEMS_PER_PAGE),
    );
    const visiblePage = Math.min(currentPage, totalPages);
    const pagedRequests = filteredRequests.slice(
        (visiblePage - 1) * ITEMS_PER_PAGE,
        visiblePage * ITEMS_PER_PAGE,
    );

    useEffect(() => {
        setCurrentPage((page) => Math.min(page, totalPages));
    }, [totalPages]);

    const toggleFavorite = (requestId: number) => {
        setFavoriteIds((currentIds) => {
            const nextIds = new Set(currentIds);

            if (nextIds.has(requestId)) {
                nextIds.delete(requestId);
            } else {
                nextIds.add(requestId);
            }

            saveFavoriteIds(nextIds);
            return nextIds;
        });
    };

    return (
        <div className="mt-3.5">
            <div className="relative">
                <span className="pointer-events-none absolute left-4 top-1/2 -translate-y-1/2 text-gray-400">
                    <SearchIcon />
                </span>
                <input
                    type="text"
                    role="searchbox"
                    value={searchTerm}
                    onChange={(event) => {
                        setSearchTerm(event.target.value);
                        setCurrentPage(1);
                    }}
                    placeholder="물품명, 출발지, 도착지 검색"
                    aria-label="매칭 요청 검색"
                    className="h-11 w-full rounded-lg border border-gray-200 bg-white pl-11 pr-10 text-sm text-gray-800 outline-none transition focus:border-purple-400 focus:ring-2 focus:ring-purple-100"
                />
                {searchTerm ? (
                    <button
                        type="button"
                        onClick={() => {
                            setSearchTerm("");
                            setCurrentPage(1);
                        }}
                        className="absolute right-2 top-1/2 flex h-8 w-8 -translate-y-1/2 items-center justify-center rounded-full text-lg text-gray-400 hover:bg-gray-100 hover:text-gray-700"
                        aria-label="검색어 초기화"
                    >
                        ×
                    </button>
                ) : null}
            </div>

            <p className="mt-2 min-h-4 text-xs font-medium text-gray-500" aria-live="polite">
                {isSearching
                    ? "검색 중..."
                    : normalizedSearchTerm
                      ? `${filteredRequests.length}개의 검색 결과`
                      : `전체 ${requests.length}개`}
            </p>

            {pagedRequests.length > 0 ? (
                <div className="mt-2 flex flex-col gap-2.5">
                    {pagedRequests.map((request) => {
                        const isFavorite = favoriteIds.has(request.id);

                        return (
                            <article
                                key={request.id}
                                className="relative grid min-h-[96px] grid-cols-[minmax(0,1fr)_76px] items-center gap-3 rounded-lg bg-gray-50 px-5 py-3 transition-colors hover:bg-gray-100"
                            >
                                <button
                                    type="button"
                                    onClick={() =>
                                        navigate(
                                            `/delivery/matching/${request.id}`,
                                        )
                                    }
                                    className="absolute inset-0 rounded-lg focus:outline-none focus-visible:ring-2 focus-visible:ring-purple-500 focus-visible:ring-offset-2"
                                    aria-label={`${request.title} 매칭 요청 상세 보기`}
                                />

                                <div className="pointer-events-none relative z-10 min-w-0">
                                    <h3 className="truncate text-sm font-bold text-gray-800">
                                        {request.title}
                                    </h3>
                                    <p className="mt-0.5 line-clamp-2 break-keep text-xs font-semibold leading-5 text-gray-500">
                                        {request.route}
                                    </p>
                                </div>

                                <div className="pointer-events-none relative z-10 flex flex-col items-end gap-2">
                                    <button
                                        type="button"
                                        onClick={() =>
                                            toggleFavorite(request.id)
                                        }
                                        className={`pointer-events-auto flex h-8 w-8 items-center justify-center rounded-full transition-colors focus:outline-none focus-visible:ring-2 focus-visible:ring-purple-500 ${
                                            isFavorite
                                                ? "bg-purple-100 text-purple-600"
                                                : "bg-white text-gray-400 hover:text-purple-500"
                                        }`}
                                        aria-label={`${request.title} 찜 ${isFavorite ? "취소" : "추가"}`}
                                        aria-pressed={isFavorite}
                                    >
                                        <FavoriteIcon selected={isFavorite} />
                                    </button>
                                    <span
                                        className="inline-flex w-[76px] items-center justify-center rounded-lg bg-purple-100 px-2 py-2 text-center text-xs font-bold text-purple-700"
                                        style={{
                                            whiteSpace: "nowrap",
                                            wordBreak: "keep-all",
                                        }}
                                    >
                                        {request.timeLeft}
                                    </span>
                                </div>
                            </article>
                        );
                    })}
                </div>
            ) : (
                <div className="mt-2 rounded-lg bg-gray-50 px-5 py-6 text-center">
                    <p className="text-sm font-semibold text-gray-600">
                        {normalizedSearchTerm
                            ? "검색 결과가 없습니다."
                            : "매칭 요청이 없습니다."}
                    </p>
                    {normalizedSearchTerm ? (
                        <button
                            type="button"
                            onClick={() => {
                                setSearchTerm("");
                                setCurrentPage(1);
                            }}
                            className="mt-2 text-xs font-bold text-purple-600 underline"
                        >
                            검색어 초기화
                        </button>
                    ) : null}
                </div>
            )}

            {filteredRequests.length > ITEMS_PER_PAGE ? (
                <nav
                    className="mt-4 flex items-center justify-center gap-1.5"
                    aria-label="매칭 요청 페이지 이동"
                >
                    <button
                        type="button"
                        onClick={() =>
                            setCurrentPage((page) => Math.max(1, page - 1))
                        }
                        disabled={visiblePage === 1}
                        className="h-8 rounded-md border border-gray-200 px-2 text-xs font-semibold text-gray-600 disabled:cursor-not-allowed disabled:opacity-35"
                    >
                        이전
                    </button>
                    {Array.from({ length: totalPages }, (_, index) => {
                        const page = index + 1;
                        return (
                            <button
                                type="button"
                                key={page}
                                onClick={() => setCurrentPage(page)}
                                aria-current={
                                    visiblePage === page ? "page" : undefined
                                }
                                className={`h-8 min-w-8 rounded-md px-2 text-xs font-bold ${
                                    visiblePage === page
                                        ? "bg-purple-600 text-white"
                                        : "border border-gray-200 bg-white text-gray-600"
                                }`}
                            >
                                {page}
                            </button>
                        );
                    })}
                    <button
                        type="button"
                        onClick={() =>
                            setCurrentPage((page) =>
                                Math.min(totalPages, page + 1),
                            )
                        }
                        disabled={visiblePage === totalPages}
                        className="h-8 rounded-md border border-gray-200 px-2 text-xs font-semibold text-gray-600 disabled:cursor-not-allowed disabled:opacity-35"
                    >
                        다음
                    </button>
                </nav>
            ) : null}
        </div>
    );
}
