import { useCallback, useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import PageHeader from "../components/common/PageHeader";
import { DeliveryInfo } from "../components/delivery/DeliveryInfo";
import { DeliveryRoute } from "../components/delivery/DeliveryRoute";
import { shipperDeliveryApi } from "../apis";
import { useApiRequest } from "../hooks/useApiRequest";
import { ReportIcon } from "../assets/icons/report";

export default function DeliveryMatchingPage() {
    const navigate = useNavigate();
    const { deliveryId: deliveryIdParam } = useParams<{
        deliveryId: string;
    }>();
    const deliveryId = Number(deliveryIdParam);
    const [isAccepting, setIsAccepting] = useState(false);
    const [acceptError, setAcceptError] = useState<string | null>(null);

    const loadDeliveryDetail = useCallback(() => {
        if (!Number.isSafeInteger(deliveryId) || deliveryId <= 0) {
            return Promise.reject(new Error("올바르지 않은 전달 ID입니다."));
        }
        return shipperDeliveryApi.getDeliveryDetail(deliveryId);
    }, [deliveryId]);

    const detailRequest = useApiRequest(loadDeliveryDetail);

    useEffect(() => {
        void detailRequest.execute().catch(() => undefined);
    }, [detailRequest.execute]);

    const handleAccept = async () => {
        if (
            !Number.isSafeInteger(deliveryId) ||
            deliveryId <= 0 ||
            isAccepting
        ) {
            setAcceptError("올바르지 않은 전달 ID입니다.");
            return;
        }

        setIsAccepting(true);
        setAcceptError(null);

        try {
            await shipperDeliveryApi.accept(deliveryId);
            navigate(`/delivery/tracking/${deliveryId}`);
        } catch (error) {
            setAcceptError(
                error instanceof Error
                    ? error.message
                    : "매칭 요청을 수락하지 못했습니다.",
            );
        } finally {
            setIsAccepting(false);
        }
    };

    if (detailRequest.isLoading) {
        return (
            <div className="page-container flex h-full flex-col">
                <PageHeader title="매칭 요청" onBack={() => navigate(-1)} />
                <p className="flex flex-1 items-center justify-center text-sm font-medium text-gray-400">
                    전달 정보를 불러오는 중입니다.
                </p>
            </div>
        );
    }

    if (detailRequest.error) {
        return (
            <div className="page-container flex h-full flex-col">
                <PageHeader title="매칭 요청" onBack={() => navigate(-1)} />
                <div className="flex flex-1 flex-col items-center justify-center gap-4 px-6 text-center">
                    <p className="text-sm font-medium text-red-500" role="alert">
                        {detailRequest.error.message ||
                            "전달 정보를 불러오지 못했습니다."}
                    </p>
                    <button
                        type="button"
                        onClick={() =>
                            void detailRequest.execute().catch(() => undefined)
                        }
                        className="rounded-lg bg-purple-600 px-4 py-2 text-sm font-semibold text-white"
                    >
                        다시 시도
                    </button>
                </div>
            </div>
        );
    }

    const delivery = detailRequest.data;

    if (!delivery) {
        return null;
    }

    return (
        <div className="page-container page-container-bottom-button relative flex flex-col">
            <PageHeader
                title="매칭 요청"
                onBack={() => navigate(-1)}
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
            <DeliveryRoute
                departure={delivery.originPlace.subwayStationName}
                destination={delivery.destPlace.subwayStationName}
            />
            <DeliveryInfo
                itemName={delivery.name}
                itemPrice={`${delivery.price.toLocaleString()}원`}
                itemSize={delivery.size}
                settlementPoint={`${delivery.totalPoint.toLocaleString()} P`}
            />
            {acceptError ? (
                <p
                    className="mb-2 text-center text-xs font-medium text-rose-600"
                    role="alert"
                >
                    {acceptError}
                </p>
            ) : null}
            <div className="absolute bottom-5 left-5 right-5 flex gap-3.5">
                <button
                    type="button"
                    onClick={() => navigate(-1)}
                    className="flex flex-1 items-center justify-center rounded-lg bg-gray-100 px-2.5 py-3.5 font-bold text-gray-600"
                >
                    거절하기
                </button>
                <button
                    type="button"
                    onClick={handleAccept}
                    disabled={isAccepting}
                    className="flex flex-1 items-center justify-center rounded-lg bg-purple-500 px-2.5 py-3.5 font-bold text-white disabled:cursor-not-allowed disabled:bg-purple-300"
                >
                    {isAccepting ? "처리 중..." : "수락하기"}
                </button>
            </div>
        </div>
    );
}
