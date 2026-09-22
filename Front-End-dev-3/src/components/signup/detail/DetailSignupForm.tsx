import { useState, type FormEvent } from "react";
import type {
    SignupFieldUpdater,
    SignupFormData,
    SignupRouteStations,
} from "../../../types/signup";
import { formatPhoneNumber } from "../../../utils/signupFormatters";
import {
    getDetailValidationMessages,
    hasDetailValidationError,
} from "../../../utils/signupValidation";
import DatePickerSheet from "../date-picker/DatePickerSheet";
import SignupSubmitButton from "../common/SignupSubmitButton";
import ValidationMessage from "../common/ValidationMessage";
import BirthDateField from "./BirthDateField";
import DetailTextField from "./DetailTextField";
import StationSelectModal, {
    type Station,
} from "../../delivery/StationSelectModal";
import { subwayApi, type SubwayStationItem } from "../../../apis/subwayApi";
import { ApiError } from "../../../types/api";

type DetailSignupFormProps = {
    formData: SignupFormData;
    updateField: SignupFieldUpdater;
    routeStations: SignupRouteStations;
    onRouteStationsChange: (stations: SignupRouteStations) => void;
    onSubmit: (event: FormEvent<HTMLFormElement>) => void;
    isSubmitting: boolean;
};

function FieldLabel({ children }: { children: string }) {
    return <p className="text-sm font-semibold text-gray-700 ml-[4px]">{children}</p>;
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

function toStation(station: SubwayStationItem): Station {
    return {
        id: station.id,
        name: station.stationName,
        region: station.routeName,
    };
}

function getRouteWayPoints(
    stations: SubwayStationItem[],
    selectedWayPointIds: number[],
) {
    const selectedIds = new Set(selectedWayPointIds);
    const routeWayPoints: Station[] = [];
    const addedIds = new Set<number>();

    stations.forEach((station, index) => {
        const isEndpoint = index === 0 || index === stations.length - 1;
        const isSelectedWayPoint = selectedIds.has(station.id);
        const isTransferStation =
            index > 0 &&
            station.routeName !== stations[index - 1].routeName;

        if (
            !isEndpoint &&
            (isSelectedWayPoint || isTransferStation) &&
            !addedIds.has(station.id)
        ) {
            routeWayPoints.push(toStation(station));
            addedIds.add(station.id);
        }
    });

    return routeWayPoints;
}

function SelectField({
    placeholder,
    value,
    region,
    onClick,
}: {
    placeholder: string;
    value?: string;
    region?: string;
    onClick: () => void;
}) {
    return (
        <button
            type="button"
            onClick={onClick}
            aria-haspopup="dialog"
            className="flex w-full items-center justify-between rounded-lg bg-gray-50 px-5 py-4 text-left"
        >
            <span className="flex min-w-0 items-baseline gap-1">
                <span
                    className={`truncate text-[15px] ${value
                        ? "font-medium text-gray-800"
                        : "text-gray-500"
                        }`}
                >
                    {value ?? placeholder}
                </span>
                {value && region ? (
                    <span className="shrink-0 text-xs font-normal text-gray-600">
                        {region}
                    </span>
                ) : null}
            </span>
            <ChevronDownIcon />
        </button>
    );
}

export default function DetailSignupForm({
    formData,
    updateField,
    routeStations,
    onRouteStationsChange,
    onSubmit,
    isSubmitting,
}: DetailSignupFormProps) {
    const [isDatePickerOpen, setIsDatePickerOpen] = useState(false);
    const [stationField, setStationField] = useState<
        "origin" | "destination" | "wayPoint" | null
    >(null);
    const [showValidation, setShowValidation] = useState(false);
    const [isSettingTransfers, setIsSettingTransfers] = useState(false);
    const [pathMessage, setPathMessage] = useState("");
    const validationMessages = getDetailValidationMessages(formData);
    const routeStationIds = [
        formData.originStationId,
        ...formData.wayPoints,
        formData.destinationStationId,
    ].filter((stationId) => stationId > 0);
    const hasDuplicateRouteStations =
        new Set(routeStationIds).size !== routeStationIds.length;
    const isDetailFormValid =
        !hasDetailValidationError(validationMessages) &&
        !hasDuplicateRouteStations;

    const handleStationSelect = (station: Station) => {
        if (stationField === "origin") {
            onRouteStationsChange({ ...routeStations, origin: station });
            updateField("originStationId", station.id);
        }

        if (stationField === "destination") {
            onRouteStationsChange({ ...routeStations, destination: station });
            updateField("destinationStationId", station.id);
        }

        if (stationField === "wayPoint") {
            const alreadySelected = routeStations.wayPoints.some(
                (wayPoint) => wayPoint.id === station.id,
            );

            if (!alreadySelected) {
                const nextWayPoints = [...routeStations.wayPoints, station];
                onRouteStationsChange({
                    ...routeStations,
                    wayPoints: nextWayPoints,
                });
                updateField(
                    "wayPoints",
                    nextWayPoints.map((wayPoint) => wayPoint.id),
                );
            }
        }

        setStationField(null);
    };

    const handleWayPointRemove = (stationId: number) => {
        const nextWayPoints = routeStations.wayPoints.filter(
            (wayPoint) => wayPoint.id !== stationId,
        );
        onRouteStationsChange({ ...routeStations, wayPoints: nextWayPoints });
        updateField(
            "wayPoints",
            nextWayPoints.map((wayPoint) => wayPoint.id),
        );
    };

    const handleAutoSetTransfers = async () => {
        if (
            formData.originStationId <= 0 ||
            formData.destinationStationId <= 0
        ) {
            setPathMessage("출발역과 도착역을 먼저 선택해주세요.");
            return;
        }

        if (formData.originStationId === formData.destinationStationId) {
            setPathMessage("출발역과 도착역은 서로 달라야 합니다.");
            return;
        }

        setIsSettingTransfers(true);
        setPathMessage("");

        try {
            const path = await subwayApi.path({
                originPlaceId: formData.originStationId,
                destinationPlaceId: formData.destinationStationId,
                waypointPlaceIds: formData.wayPoints,
            });
            const nextWayPoints = getRouteWayPoints(
                path.stations,
                formData.wayPoints,
            );

            onRouteStationsChange({
                ...routeStations,
                wayPoints: nextWayPoints,
            });
            updateField(
                "wayPoints",
                nextWayPoints.map((wayPoint) => wayPoint.id),
            );
            setPathMessage(
                path.transferCount > 0
                    ? `환승역 ${path.transferCount}개를 자동으로 설정했습니다.`
                    : "환승이 필요 없는 경로입니다.",
            );
        } catch (error) {
            setPathMessage(
                error instanceof ApiError
                    ? error.message
                    : "환승역을 자동 설정하는 중 오류가 발생했습니다.",
            );
        } finally {
            setIsSettingTransfers(false);
        }
    };

    const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
        event.preventDefault();
        setShowValidation(true);

        if (hasDetailValidationError(validationMessages)) {
            return;
        }

        onSubmit(event);
    };

    return (
        <form onSubmit={handleSubmit} className="flex min-h-0 flex-1 flex-col">
            <div className="scrollbar-hidden flex flex-1 flex-col gap-5 overflow-y-auto pb-6 pt-[38px]">
                <DetailTextField
                    id="signup-name"
                    label="이름"
                    type="text"
                    placeholder="이름을 입력해주세요"
                    value={formData.name}
                    validationMessage={validationMessages.name}
                    validationFallback="이름 검증 메시지"
                    showValidation={showValidation}
                    onChange={(value) => updateField("name", value)}
                />
                <DetailTextField
                    id="signup-phone"
                    label="전화번호"
                    type="tel"
                    placeholder="전화번호를 입력해주세요"
                    value={formData.phone}
                    validationMessage={validationMessages.phone}
                    validationFallback="전화번호 검증 메시지"
                    showValidation={showValidation}
                    onChange={(value) =>
                        updateField("phone", formatPhoneNumber(value))
                    }
                />
                <BirthDateField
                    value={formData.birthDate}
                    validationMessage={validationMessages.birthDate}
                    showValidation={showValidation}
                    onOpen={() => setIsDatePickerOpen(true)}
                />
                <div className="flex flex-col gap-[4px]">
                    <FieldLabel>통학로 설정</FieldLabel>
                    <SelectField
                        placeholder="출발지를 선택해주세요"
                        value={routeStations.origin?.name}
                        region={routeStations.origin?.region}
                        onClick={() => setStationField("origin")}
                    />
                    {showValidation &&
                        Boolean(validationMessages.originStation) && (
                            <ValidationMessage
                                message={validationMessages.originStation}
                                fallback=""
                            />
                        )}

                    {routeStations.wayPoints.map((wayPoint) => (
                        <div
                            key={wayPoint.id}
                            className="flex items-center justify-between rounded-lg bg-purple-100 px-5 py-3"
                        >
                            <span className="flex min-w-0 items-baseline gap-1">
                                <span className="truncate text-[15px] font-medium text-gray-800">
                                    {wayPoint.name}
                                </span>
                                <span className="shrink-0 text-xs font-normal text-gray-700">
                                    {wayPoint.region}
                                </span>
                            </span>
                            <button
                                type="button"
                                onClick={() => handleWayPointRemove(wayPoint.id)}
                                className="text-xs font-semibold text-gray-600"
                                aria-label={`${wayPoint.name} 경유지 삭제`}
                            >
                                삭제
                            </button>
                        </div>
                    ))}

                    <div className="flex flex-row gap-[10px] justify-center items-center mt-[16px] mb-[16px]">
                        <button
                            type="button"
                            onClick={() => setStationField("wayPoint")}
                            className="w-[100px] text-right font-semibold text-[13px] text-gray-500"
                        >
                            경유지 추가
                        </button>
                        <div className="w-2  font-semibold text-[13px] text-gray-200">|</div>
                        <button
                            type="button"
                            onClick={handleAutoSetTransfers}
                            disabled={isSettingTransfers}
                            className="w-[100px] text-left font-semibold text-[13px] text-purple-500 disabled:cursor-wait disabled:text-gray-400"
                        >
                            {isSettingTransfers ? "경로 탐색 중..." : "환승역 자동설정"}
                        </button>
                    </div>

                    {pathMessage ? (
                        <p
                            className={`px-1 text-xs ${pathMessage.includes("오류") ||
                                pathMessage.includes("선택") ||
                                pathMessage.includes("달라야")
                                ? "text-red-500"
                                : "text-purple-600"
                                }`}
                        >
                            {pathMessage}
                        </p>
                    ) : null}

                    <SelectField
                        placeholder="도착지를 선택해주세요"
                        value={routeStations.destination?.name}
                        region={routeStations.destination?.region}
                        onClick={() => setStationField("destination")}
                    />
                    <ValidationMessage
                        message={validationMessages.destinationStation}
                        fallback=""
                        visible={
                            showValidation &&
                            Boolean(validationMessages.destinationStation)
                        }
                    />
                </div>
            </div>

            <div className="shrink-0 pt-4 [&>button]:mt-0">
                <SignupSubmitButton
                    disabled={
                        !isDetailFormValid ||
                        isSubmitting ||
                        isSettingTransfers
                    }
                >
                    {isSubmitting ? "가입 처리 중..." : "회원 가입 완료"}
                </SignupSubmitButton>
            </div>

            {isDatePickerOpen ? (
                <DatePickerSheet
                    value={formData.birthDate}
                    onClose={() => setIsDatePickerOpen(false)}
                    onConfirm={(nextDate) => {
                        updateField("birthDate", nextDate);
                        setIsDatePickerOpen(false);
                    }}
                />
            ) : null}

            {stationField ? (
                <StationSelectModal
                    title={
                        stationField === "origin"
                            ? "출발역 선택"
                            : stationField === "destination"
                                ? "도착역 선택"
                                : "경유역 선택"
                    }
                    onClose={() => setStationField(null)}
                    onSelect={handleStationSelect}
                />
            ) : null}
        </form>
    );
}
