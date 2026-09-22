import { memo, useEffect, useState } from "react";
import DeliveryPaymentSheet from "./DeliveryPaymentSheet";
import { CameraIcon } from "../../assets/icons/CameraIcon";
import StationSelectModal, { type Station } from "./StationSelectModal";
import { ApiError } from "../../types/api";
import PageHeader from "../common/PageHeader";
import { SizeInfo } from "./SizeInfo";
import { DeliveryImageUploader } from "./DeliveryImageUploader";
import { useDebouncedValue } from "../../hooks/useDebouncedValue";
import {
    CreateDeliveryRequest,
    DeliveryPayment,
} from "../../types/delivery/sender";
import { senderDeliveryApi } from "../../apis";

const ITEM_PRICE_PATTERN = /^[0-9]+$/;
const MAX_ITEM_PRICE_WON = 5_000_000;

function sanitizeItemPrice(value: string) {
    return value.replace(/\D/g, "");
}

function getItemPriceValidationMessage(value: string) {
    const trimmedPrice = value.trim();

    if (trimmedPrice === "") {
        return "물품 가액을 입력해주세요.";
    }

    if (!ITEM_PRICE_PATTERN.test(trimmedPrice)) {
        return "0 이상의 정수만 입력해주세요.";
    }

    const priceInWon = Number(trimmedPrice);

    if (!Number.isSafeInteger(priceInWon)) {
        return "입력할 수 있는 물품 가액을 초과했습니다.";
    }

    if (priceInWon > MAX_ITEM_PRICE_WON) {
        return "물품 가액은 최대 500만원까지 입력할 수 있습니다.";
    }

    return "";
}

function getApiErrorMessage(error: unknown, fallback: string) {
    return error instanceof ApiError ? error.message : fallback;
}

interface DeliveryRequestFormProps {
    isLoading?: boolean;
    error?: string | null;
    onRetry?: () => void;
    onBack?: () => void;
    onComplete?: (deliveryId: number) => void;
}

function ChevronDownIcon() {
    return (
        <svg
            width="14"
            height="8"
            viewBox="0 0 14 8"
            fill="none"
            aria-hidden="true"
        >
            <path
                d="M1 1L7 7L13 1"
                stroke="#373840"
                strokeWidth="1.6"
                strokeLinecap="round"
                strokeLinejoin="round"
            />
        </svg>
    );
}

function FieldLabel({ children }: { children: string }) {
    return <p className="text-sm font-semibold text-gray-700">{children}</p>;
}

function SelectField({
    placeholder,
    value,
    error,
    onClick,
}: {
    placeholder: string;
    value?: string;
    error?: string;
    onClick: () => void;
}) {
    return (
        <div className="flex flex-col gap-1">
            <button
                type="button"
                onClick={onClick}
                aria-haspopup="dialog"
                className="flex w-full items-center justify-between rounded-lg bg-gray-50 px-5 py-4 text-left"
            >
                <span
                    className={`text-[15px] ${
                        value ? "font-medium text-gray-900" : "text-gray-500"
                    }`}
                >
                    {value ?? placeholder}
                </span>
                <ChevronDownIcon />
            </button>
            {error ? (
                <p className="text-xs font-medium text-rose-600">{error}</p>
            ) : null}
        </div>
    );
}

function SizeSelectField({
    selected,
    onSelect,
    error,
}: {
    selected: CreateDeliveryRequest["size"] | null;
    onSelect: (value: CreateDeliveryRequest["size"]) => void;
    error?: string;
}) {
    const [isOpen, setIsOpen] = useState(false);
    const options: CreateDeliveryRequest["size"][] = ["S", "M", "L"];

    return (
        <div className="flex flex-col gap-1">
            <div className="relative">
                <button
                    type="button"
                    onClick={() => setIsOpen((prev) => !prev)}
                    className="flex h-[52px] w-full items-center justify-between rounded-[10px] bg-[#F8F9FD] px-5 text-left"
                >
                    <span
                        className={
                            selected
                                ? "text-[15px] font-medium text-[#373840]"
                                : "text-[15px] font-medium text-[#8E91A1]"
                        }
                    >
                        {selected ?? "물품 크기를 선택해주세요"}
                    </span>
                    <ChevronDownIcon />
                </button>

                {isOpen ? (
                    <div className="absolute inset-x-0 top-[58px] z-20 overflow-hidden rounded-[10px] border border-[#EDEEF3] bg-white shadow-lg">
                        {options.map((option) => (
                            <button
                                key={option}
                                type="button"
                                onMouseDown={(event) => event.preventDefault()}
                                onClick={() => {
                                    onSelect(option);
                                    setIsOpen(false);
                                }}
                                className="flex h-11 w-full items-center px-5 text-[15px] font-medium text-[#373840] hover:bg-[#F8F9FD]"
                            >
                                {option}
                            </button>
                        ))}
                    </div>
                ) : null}
            </div>
            {error ? (
                <p className="text-xs font-medium text-rose-600">{error}</p>
            ) : null}
        </div>
    );
}
function TextField({
    placeholder,
    value,
    onChange,
    error,
    language,
}: {
    placeholder: string;
    value: string;
    onChange: (value: string) => void;
    error?: string;
    language?: string;
}) {
    return (
        <div className="flex flex-col gap-1">
            <input
                type="text"
                inputMode="text"
                lang={language}
                autoComplete="off"
                autoCapitalize="none"
                spellCheck={false}
                value={value}
                onChange={(event) => onChange(event.target.value)}
                placeholder={placeholder}
                className="w-full rounded-lg bg-gray-50 px-5 py-4 text-[15px] text-gray-800 placeholder:text-gray-500 focus:outline-none"
            />
            {error ? (
                <p className="text-xs font-medium text-rose-600">{error}</p>
            ) : null}
        </div>
    );
}

function PriceField({
    value,
    onChange,
    error,
}: {
    value: string;
    onChange: (value: string) => void;
    error?: string;
}) {
    return (
        <div className="flex flex-col gap-1">
            <div className="flex w-full items-center justify-between rounded-lg bg-gray-50 px-5 py-4">
                <input
                    type="text"
                    inputMode="numeric"
                    pattern="[0-9]*"
                    value={value}
                    onChange={(event) =>
                        onChange(sanitizeItemPrice(event.target.value))
                    }
                    placeholder="물품가액을 입력해주세요"
                    aria-invalid={Boolean(error)}
                    className="w-full bg-transparent text-[15px] text-gray-800 placeholder:text-gray-500 focus:outline-none"
                />
                <span className="shrink-0 text-[15px] text-gray-800">원</span>
            </div>
            {error ? (
                <p className="text-xs font-medium text-rose-600">{error}</p>
            ) : null}
        </div>
    );
}

function LoadingDeliveryRequestForm() {
    return (
        <section
            className="mx-auto flex h-full w-full max-w-[402px] flex-col bg-white"
            aria-busy="true"
            aria-labelledby="delivery-request-loading-title"
        >
            <h1 id="delivery-request-loading-title" className="sr-only">
                전달 요청 정보를 불러오는 중
            </h1>
            <div className="flex h-14 shrink-0 items-center justify-center px-5">
                <div className="h-5 w-16 animate-pulse rounded bg-slate-200" />
            </div>
            <div className="scrollbar-hidden flex-1 overflow-x-hidden overflow-y-auto px-5 pb-6 pt-4">
                <div className="flex flex-col gap-5">
                    {Array.from({ length: 6 }, (_, index) => (
                        <div key={index} className="flex flex-col gap-[10px]">
                            <div className="h-4 w-16 animate-pulse rounded bg-slate-200" />
                            <div className="h-[52px] w-full animate-pulse rounded-[10px] bg-slate-100" />
                        </div>
                    ))}
                </div>
            </div>
            <p className="sr-only" role="status">
                전달 요청 화면을 불러오고 있습니다.
            </p>
        </section>
    );
}

function ErrorDeliveryRequestForm({
    message,
    onRetry,
}: {
    message: string;
    onRetry?: () => void;
}) {
    return (
        <section className="mx-auto flex h-full w-full max-w-[402px] items-center bg-white px-5 py-6">
            <div
                className="w-full rounded-lg border border-rose-200 bg-rose-50 p-5 text-rose-900"
                role="alert"
            >
                <h1 className="text-lg font-semibold">
                    전달 요청 화면을 불러오지 못했습니다
                </h1>
                <p className="mt-2 text-sm">{message}</p>
                {onRetry ? (
                    <button
                        type="button"
                        className="mt-4 rounded-md bg-rose-700 px-4 py-2 text-sm font-semibold text-white transition hover:bg-rose-800 focus:outline-none"
                        onClick={onRetry}
                    >
                        다시 시도
                    </button>
                ) : null}
            </div>
        </section>
    );
}

function DeliveryRequestFormContent({
    onBack,
    onComplete,
}: Pick<DeliveryRequestFormProps, "onBack" | "onComplete">) {
    const [isPaymentOpen, setIsPaymentOpen] = useState(false);
    const [createdDeliveryId, setCreatedDeliveryId] = useState<number | null>(
        null,
    );
    const [pendingRequest, setPendingRequest] =
        useState<CreateDeliveryRequest | null>(null);
    const [payment, setPayment] = useState<DeliveryPayment | null>(null);
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [isConfirming, setIsConfirming] = useState(false);
    const [requestError, setRequestError] = useState("");
    const [paymentError, setPaymentError] = useState("");
    const [stationField, setStationField] = useState<
        "origin" | "destination" | null
    >(null);
    const [originStation, setOriginStation] = useState<Station | null>(null);
    const [destinationStation, setDestinationStation] =
        useState<Station | null>(null);
    const [originError, setOriginError] = useState("");
    const [destinationError, setDestinationError] = useState("");
    const isOverlayOpen = isPaymentOpen || stationField !== null;

    const handleStationSelect = (station: Station) => {
        if (stationField === "origin") {
            setOriginStation(station);
            setOriginError("");
        }

        if (stationField === "destination") {
            setDestinationStation(station);
            setDestinationError("");
        }

        setStationField(null);
    };

    const handleStationModalClose = () => {
        if (stationField === "origin" && !originStation) {
            setOriginError("출발지를 선택해주세요.");
        }

        if (stationField === "destination" && !destinationStation) {
            setDestinationError("도착지를 선택해주세요.");
        }

        setStationField(null);
    };

    const [itemName, setItemName] = useState("");
    const [itemNameError, setItemNameError] = useState("");
    const [itemPrice, setItemPrice] = useState("");
    const [itemPriceError, setItemPriceError] = useState("");
    const [itemSize, setItemSize] = useState<
        CreateDeliveryRequest["size"] | null
    >(null);
    const [itemSizeError, setItemSizeError] = useState("");
    const [memo, setMemo] = useState("");
    const [hasSubmitted, setHasSubmitted] = useState(false);

    const debouncedItemName = useDebouncedValue(itemName, 300);
    const debouncedItemPrice = useDebouncedValue(itemPrice, 300);

    useEffect(() => {
        if (!hasSubmitted) {
            return;
        }

        setItemNameError(
            debouncedItemName.trim() === "" ? "물품명을 입력해주세요." : "",
        );
    }, [debouncedItemName, hasSubmitted]);

    useEffect(() => {
        if (!hasSubmitted) {
            return;
        }

        setItemPriceError(getItemPriceValidationMessage(debouncedItemPrice));
    }, [debouncedItemPrice, hasSubmitted]);

    const handleMatchingRequest = async () => {
        setHasSubmitted(true);
        setRequestError("");

        let hasError = false;

        if (!originStation) {
            setOriginError("출발지를 선택해주세요.");
            hasError = true;
        } else {
            setOriginError("");
        }

        if (!destinationStation) {
            setDestinationError("도착지를 선택해주세요.");
            hasError = true;
        } else if (destinationStation.id === originStation?.id) {
            setDestinationError("출발지와 도착지는 같을 수 없습니다.");
            hasError = true;
        } else {
            setDestinationError("");
        }

        if (itemName.trim() === "") {
            setItemNameError("물품명을 입력해주세요.");
            hasError = true;
        } else {
            setItemNameError("");
        }

        const trimmedPrice = itemPrice.trim();

        const priceInWon = Number(trimmedPrice);

        const priceError = getItemPriceValidationMessage(trimmedPrice);

        if (priceError) {
            setItemPriceError(priceError);
            hasError = true;
        } else {
            setItemPriceError("");
        }

        if (!itemSize) {
            setItemSizeError("물품 크기를 선택해주세요.");
            hasError = true;
        } else {
            setItemSizeError("");
        }

        if (hasError) {
            return;
        }

        if (!originStation || !destinationStation || !itemSize) {
            return;
        }

        if (createdDeliveryId !== null && payment) {
            setPaymentError("");
            setIsPaymentOpen(true);
            return;
        }

        const request: CreateDeliveryRequest = {
            sourceStationId: originStation.id,
            destinationStationId: destinationStation.id,
            name: itemName.trim(),
            price: priceInWon,
            size: itemSize,
            memo: memo.trim() || undefined,
        };

        setIsSubmitting(true);

        try {
            const paymentResult = await senderDeliveryApi.getPayment({
                sourceStationId: request.sourceStationId,
                destinationStationId: request.destinationStationId,
                size: request.size,
            });
            setPendingRequest(request);
            setPayment(paymentResult);
            setPaymentError("");
            setIsPaymentOpen(true);
        } catch (error) {
            setRequestError(
                getApiErrorMessage(
                    error,
                    "결제 정보를 불러오지 못했습니다. 다시 시도해주세요.",
                ),
            );
        } finally {
            setIsSubmitting(false);
        }
    };

    const handlePaymentConfirm = async () => {
        if (!pendingRequest || isConfirming) {
            return;
        }

        setIsConfirming(true);
        setPaymentError("");

        try {
            let deliveryId = createdDeliveryId;

            if (deliveryId === null) {
                const createdId =
                    await senderDeliveryApi.create(pendingRequest);
                deliveryId = Number(createdId);

                if (!Number.isSafeInteger(deliveryId) || deliveryId <= 0) {
                    throw new Error("Invalid delivery ID response");
                }

                setCreatedDeliveryId(deliveryId);
            }

            await senderDeliveryApi.agreeTerms(deliveryId);
            onComplete?.(deliveryId);
        } catch (error) {
            setPaymentError(
                getApiErrorMessage(
                    error,
                    "결제를 완료하지 못했습니다. 다시 시도해주세요.",
                ),
            );
        } finally {
            setIsConfirming(false);
        }
    };

    return (
        <div className="page-container relative flex h-full min-h-0 flex-col bg-white">
            <div
                className={`flex min-h-0 flex-1 flex-col transition duration-100 ${
                    isOverlayOpen ? "pointer-events-none blur-sm" : ""
                }`}
                aria-hidden={isOverlayOpen}
            >
                <PageHeader
                    title="전달 요청"
                    onBack={onBack}
                    className="shrink-0"
                />

                <fieldset
                    disabled={createdDeliveryId !== null}
                    className="scrollbar-hidden m-0 min-w-0 min-h-0 flex-1 overflow-x-hidden overflow-y-auto overscroll-contain border-0 pb-16 pt-4"
                >
                    <div className="flex flex-col gap-5">
                        <div className="flex flex-col gap-[10px]">
                            <FieldLabel>출발지</FieldLabel>
                            <SelectField
                                placeholder="출발지를 선택해주세요"
                                value={originStation?.name}
                                error={originError}
                                onClick={() => setStationField("origin")}
                            />
                        </div>

                        <div className="flex flex-col gap-[10px]">
                            <FieldLabel>도착지</FieldLabel>
                            <SelectField
                                placeholder="도착지를 선택해주세요"
                                value={destinationStation?.name}
                                error={destinationError}
                                onClick={() => setStationField("destination")}
                            />
                        </div>

                        <div className="flex flex-col gap-[10px]">
                            <FieldLabel>물품명</FieldLabel>
                            <TextField
                                placeholder="물품명을 입력해주세요"
                                value={itemName}
                                onChange={setItemName}
                                error={itemNameError}
                            />
                        </div>

                        <div className="flex flex-col gap-[10px]">
                            <FieldLabel>물품가액</FieldLabel>
                            <PriceField
                                value={itemPrice}
                                onChange={setItemPrice}
                                error={itemPriceError}
                            />
                        </div>

                        <div className="flex flex-col gap-[10px]">
                            <div className="flex items-center gap-1">
                                <FieldLabel>물품 크기</FieldLabel>
                                <div className="group relative flex items-center justify-center w-4 h-4 rounded-full bg-purple-400 text-white text-xs font-bold cursor-pointer">
                                    ?
                                    <SizeInfo
                                        boxTranslate="-18%"
                                        arrowLeft="18%"
                                    />
                                </div>
                            </div>
                            <SizeSelectField
                                selected={itemSize}
                                onSelect={(size) => {
                                    setItemSize(size);
                                    setItemSizeError("");
                                }}
                                error={itemSizeError}
                            />
                        </div>

                        <div className="flex flex-col gap-[10px]">
                            <FieldLabel>사진 등록</FieldLabel>
                            <DeliveryImageUploader />
                        </div>

                        <div className="flex flex-col gap-[10px]">
                            <FieldLabel>메모</FieldLabel>
                            <TextField
                                placeholder="메모를 입력해주세요"
                                value={memo}
                                onChange={setMemo}
                                language="ko"
                            />
                        </div>
                    </div>
                </fieldset>

                <div className="flex shrink-0 flex-col pb-2 pt-2">
                    {createdDeliveryId !== null ? (
                        <p className="mb-2 text-center text-xs font-medium text-purple-600">
                            전달 요청이 생성되었습니다. 결제를 완료해주세요.
                        </p>
                    ) : null}
                    {requestError ? (
                        <p
                            className="mb-2 text-center text-xs font-medium text-rose-600"
                            role="alert"
                        >
                            {requestError}
                        </p>
                    ) : null}
                    <button
                        type="button"
                        onClick={handleMatchingRequest}
                        className="flex w-full items-center justify-center rounded-lg bg-purple-500 py-3.5 font-bold leading-[22px] text-white transition hover:bg-purple-600 focus:outline-none"
                    >
                        {isSubmitting
                            ? "결제 정보 확인 중..."
                            : createdDeliveryId !== null
                              ? "결제 계속하기"
                              : "매칭 요청"}
                    </button>
                </div>
            </div>

            {isPaymentOpen && payment ? (
                <DeliveryPaymentSheet
                    payment={payment}
                    isConfirming={isConfirming}
                    errorMessage={paymentError}
                    onClose={() => setIsPaymentOpen(false)}
                    onConfirm={handlePaymentConfirm}
                />
            ) : null}

            {stationField ? (
                <StationSelectModal
                    title={
                        stationField === "origin"
                            ? "출발역 선택"
                            : "도착역 선택"
                    }
                    onClose={handleStationModalClose}
                    onSelect={handleStationSelect}
                />
            ) : null}
        </div>
    );
}

function DeliveryRequestForm({
    isLoading = false,
    error = null,
    onRetry,
    onBack,
    onComplete,
}: DeliveryRequestFormProps) {
    if (isLoading) {
        return <LoadingDeliveryRequestForm />;
    }

    if (error) {
        return <ErrorDeliveryRequestForm message={error} onRetry={onRetry} />;
    }

    return (
        <DeliveryRequestFormContent onBack={onBack} onComplete={onComplete} />
    );
}

export default memo(DeliveryRequestForm);
