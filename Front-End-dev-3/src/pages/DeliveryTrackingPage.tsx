import { useCallback, useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { subwayApi } from "../apis/subwayApi";
import PageHeader from "../components/common/PageHeader";
import { DeliveryTrackingContent } from "../components/delivery/DeliveryTrackingContent";
import { DeliveryTrackingProgress } from "../components/delivery/DeliveryTrackingOverview";
import matchingWaitImage from "../assets/matching-wait.svg";
import { useApiRequest } from "../hooks/useApiRequest";
import { useShipperRouteTracking } from "../hooks/useShipperRouteTracking";
import { ApiError } from "../types/api";
import type { BackendDeliveryState } from "../types/backend";
import { shipperDeliveryApi } from "../apis";
import { ReportIcon } from "../assets/icons/report";
import { DeliveryImageUploader } from "../components/delivery/DeliveryImageUploader";

function getShipperTrackingMessage(status: BackendDeliveryState) {
    switch (status) {
        case "WAIT":
            return "전달 매칭 전 상태입니다.";
        case "MATCHED":
            return "물품 인수 후 GPS 위치 공유가 시작됩니다.";
        case "DELIVERING":
            return "내 GPS 위치를 경로 위에 실시간으로 표시하고 있습니다.";
        case "CONFIRM_REQUESTED":
            return "발송자의 최종 완료 승인을 기다리고 있습니다.";
        case "DELIVERED":
            return "전달이 최종 완료되었습니다.";
        case "CANCEL":
            return "취소된 전달입니다.";
    }
}

function getActionLabel(status: BackendDeliveryState) {
    return status === "MATCHED" ? "물품 인수 확인" : "완료 확인";
}

export default function DeliveryTrackingPage() {
    const navigate = useNavigate();
    const { deliveryId: deliveryIdParam } = useParams<{
        deliveryId: string;
    }>();
    const deliveryId = Number(deliveryIdParam);
    const [isUpdatingStatus, setIsUpdatingStatus] = useState(false);
    const [isUploadingActionImage, setIsUploadingActionImage] =
        useState(false);
    const [actionImageKeys, setActionImageKeys] = useState<string[]>([]);
    const [actionError, setActionError] = useState<string | null>(null);
    const loadTrackingData = useCallback(async () => {
        if (!Number.isSafeInteger(deliveryId) || deliveryId <= 0) {
            throw new ApiError({ message: "올바르지 않은 전달 ID입니다." });
        }

        const detail = await shipperDeliveryApi.getDeliveryDetail(deliveryId);
        if (
            detail.deliveryState === "WAIT" ||
            detail.deliveryState === "DELIVERED"
        ) {
            return { detail, route: null };
        }

        const route = await subwayApi.path({
            originPlaceId: detail.originPlace.id,
            destinationPlaceId: detail.destPlace.id,
            waypointPlaceIds: [],
        });

        return { detail, route };
    }, [deliveryId]);
    const { data, error, isLoading, execute } = useApiRequest(loadTrackingData);
    const locationTracking = useShipperRouteTracking({
        enabled: data?.detail.deliveryState === "DELIVERING",
        stations: data?.route?.stations ?? [],
    });

    useEffect(() => {
        void execute().catch(() => undefined);
    }, [execute]);

    const handleStatusAction = async () => {
        if (!data || isUpdatingStatus || isUploadingActionImage) {
            return;
        }

        const status = data.detail.deliveryState;
        if (status !== "MATCHED" && status !== "DELIVERING") {
            return;
        }

        setIsUpdatingStatus(true);
        setActionError(null);

        try {
            const request = actionImageKeys[0]
                ? { imageKey: actionImageKeys[0] }
                : undefined;

            if (status === "MATCHED") {
                await shipperDeliveryApi.acquire(deliveryId, request);
            } else {
                await shipperDeliveryApi.requestConfirmation(
                    deliveryId,
                    request,
                );
            }

            await execute();
            setActionImageKeys([]);
        } catch (caughtError) {
            setActionError(
                caughtError instanceof Error
                    ? caughtError.message
                    : "전달 상태를 변경하지 못했습니다.",
            );
        } finally {
            setIsUpdatingStatus(false);
        }
    };

    if (isLoading || (!data && !error)) {
        return (
            <div className="page-container flex h-full min-h-0 flex-col overflow-hidden">
                <PageHeader
                    title="전달 추적"
                    onBack={() => navigate(-1)}
                    className="shrink-0"
                    rightAction={
                        <button
                            type="button"
                            onClick={() =>
                                navigate("/report", {
                                    state: {
                                        targetType: "DELIVERY",
                                        deliveryId,
                                    },
                                })
                            }
                        >
                            <button className="text-gray-500">
                                <ReportIcon />
                            </button>
                        </button>
                    }
                />
                <div
                    className="flex flex-1 flex-col gap-5 pt-8"
                    aria-busy="true"
                >
                    <div className="h-52 animate-pulse rounded-3xl bg-gray-100" />
                    <div className="h-96 animate-pulse rounded-3xl bg-gray-100" />
                </div>
            </div>
        );
    }

    if (error || !data) {
        return (
            <div className="page-container flex h-full min-h-0 flex-col overflow-hidden">
                <PageHeader
                    title="전달 추적"
                    onBack={() => navigate(-1)}
                    className="shrink-0"
                    rightAction={
                        <button
                            type="button"
                            onClick={() =>
                                navigate("/report", {
                                    state: {
                                        targetType: "DELIVERY",
                                        deliveryId,
                                    },
                                })
                            }
                        >
                            <button className="text-gray-500">
                                <ReportIcon />
                            </button>
                        </button>
                    }
                />
                <div className="flex flex-1 items-center justify-center">
                    <div
                        className="w-full rounded-2xl border border-rose-200 bg-rose-50 p-5 text-center"
                        role="alert"
                    >
                        <p className="text-sm font-medium text-rose-700">
                            {error?.message ??
                                "전달 추적 정보를 불러오지 못했습니다."}
                        </p>
                        <button
                            type="button"
                            onClick={() =>
                                void execute().catch(() => undefined)
                            }
                            className="mt-4 text-sm font-bold text-rose-700 underline"
                        >
                            다시 시도
                        </button>
                    </div>
                </div>
            </div>
        );
    }

    const { detail, route } = data;
    const canUpdateStatus =
        detail.deliveryState === "MATCHED" ||
        detail.deliveryState === "DELIVERING";
    const isWaitingForMatch = detail.deliveryState === "WAIT";

    return (
        <div className="page-container relative flex h-full min-h-0 flex-col overflow-hidden">
            <PageHeader
                title="전달 추적"
                onBack={() => navigate(-1)}
                className="shrink-0"
                rightAction={
                    <button
                        type="button"
                        onClick={() =>
                            navigate("/report", {
                                state: {
                                    targetType: "DELIVERY",
                                    deliveryId,
                                },
                            })
                        }
                    >
                        <button className="text-gray-500">
                            <ReportIcon />
                        </button>
                    </button>
                }
            />

            {isWaitingForMatch ? (
                <div className="flex min-h-0 flex-1 flex-col pt-7">
                    <DeliveryTrackingProgress status="WAIT" />
                    <div className="flex flex-1 flex-col items-center justify-center pb-12 text-center">
                        <img
                            src={matchingWaitImage}
                            alt=""
                            className="h-[106px] w-[162px]"
                        />
                        <div className="mt-[38px]">
                            <h2 className="text-[22px] font-semibold leading-[22px] text-purple-600">
                                전달자를 찾는 중이에요!
                            </h2>
                            <p className="mt-[15px] text-sm font-medium leading-normal text-gray-400">
                                잠시만 기다려주세요...
                                <br />
                                빠르게 매칭해 드릴게요!
                            </p>
                        </div>
                    </div>
                </div>
            ) : (
                <div className="scrollbar-hidden flex-1 overflow-y-auto pb-6">
                    <DeliveryTrackingContent
                        itemName={detail.name}
                        status={detail.deliveryState}
                        originPlace={detail.originPlace}
                        destinationPlace={detail.destPlace}
                        logs={detail.deliveryTimeLine}
                        route={route}
                        isRouteLoading={false}
                        currentPlaceId={
                            locationTracking.nearestStation?.station.id
                        }
                        currentCoordinates={
                            locationTracking.position ?? undefined
                        }
                        currentDistanceMeters={
                            locationTracking.nearestStation?.distanceMeters
                        }
                        gpsAccuracyMeters={
                            locationTracking.position?.accuracyMeters
                        }
                        trackingStatus={locationTracking.status}
                        trackingError={locationTracking.errorMessage}
                        lastLocationUpdatedAt={
                            locationTracking.lastSyncedLocation?.updatedAt
                        }
                        overviewTimeMode="elapsed"
                        trackingStatusMessage={getShipperTrackingMessage(
                            detail.deliveryState,
                        )}
                        partyTitle="발송자 정보"
                        party={detail.senderInfo}
                        partyEmptyMessage="발송자 정보를 확인할 수 없습니다."
                        onPartyMessageClick={() =>
                            navigate(`/delivery/chat/${deliveryId}`)
                        }
                        variant="figma"
                    />
                </div>
            )}

            {isWaitingForMatch ? (
                <div className="shrink-0 bg-white pt-3">
                    <button
                        type="button"
                        disabled
                        className="w-full rounded-[10px] bg-gray-100 py-3.5 font-semibold text-gray-900"
                    >
                        완료 확인
                    </button>
                </div>
            ) : canUpdateStatus ? (
                <div className="shrink-0 bg-white pt-3">
                    <div className="mb-3">
                        <p className="mb-2 text-sm font-semibold text-gray-700">
                            {detail.deliveryState === "MATCHED"
                                ? "물품 인수 사진"
                                : "물품 전달 사진"}
                            <span className="ml-1 font-normal text-gray-400">
                                (선택)
                            </span>
                        </p>
                        <DeliveryImageUploader
                            key={`${deliveryId}-${detail.deliveryState}`}
                            maxImages={1}
                            onChange={setActionImageKeys}
                            onUploadingChange={setIsUploadingActionImage}
                        />
                    </div>
                    {actionError ? (
                        <p
                            className="mb-2 text-center text-xs font-medium text-rose-600"
                            role="alert"
                        >
                            {actionError}
                        </p>
                    ) : null}
                    <button
                        type="button"
                        onClick={handleStatusAction}
                        disabled={
                            isUpdatingStatus || isUploadingActionImage
                        }
                        className="w-full rounded-[10px] bg-gray-100 py-3.5 font-semibold text-gray-900 shadow-[0_4px_15px_rgba(0,0,0,0.05)] transition-colors hover:bg-gray-200 disabled:cursor-not-allowed disabled:text-gray-500"
                    >
                        {isUploadingActionImage
                            ? "사진 업로드 중..."
                            : isUpdatingStatus
                            ? "처리 중..."
                            : getActionLabel(detail.deliveryState)}
                    </button>
                </div>
            ) : null}
        </div>
    );
}
