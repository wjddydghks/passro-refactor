import { useCallback, useEffect, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import { accountApi } from "../apis/accountApi";
import { fileApi } from "../apis/fileApi";
import { subwayApi, type SubwayStationItem } from "../apis/subwayApi";
import { CameraIcon } from "../assets/icons/CameraIcon";
import PageHeader from "../components/common/PageHeader";
import StationSelectModal, {
    type Station,
} from "../components/delivery/StationSelectModal";
import BirthDateField from "../components/signup/detail/BirthDateField";
import DatePickerSheet from "../components/signup/date-picker/DatePickerSheet";
import { useApiRequest } from "../hooks/useApiRequest";
import { ApiError } from "../types/api";
import { formatPhoneNumber } from "../utils/signupFormatters";
import { getCurrentUser, updateCurrentUserProfile } from "../utils/auth";
import { imgproxied } from "../utils/img";

const editableFieldClassName =
    "min-h-[52px] w-full rounded-[10px] px-5 py-[15px] text-[15px] font-medium leading-[22px] outline-none placeholder:text-gray-500";

type StationField = "origin" | "destination" | "wayPoint";

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
            index > 0 && station.routeName !== stations[index - 1].routeName;

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

function MoveIcon() {
    return (
        <svg
            width="8"
            height="14"
            viewBox="0 0 8 14"
            fill="none"
            aria-hidden="true"
        >
            <path
                d="M1 1L7 7L1 13"
                stroke="#B3B5C1"
                strokeWidth="1.6"
                strokeLinecap="round"
                strokeLinejoin="round"
            />
        </svg>
    );
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

function StationFieldButton({
    placeholder,
    station,
    onClick,
}: {
    placeholder: string;
    station: Station | null;
    onClick: () => void;
}) {
    return (
        <button
            type="button"
            onClick={onClick}
            aria-haspopup="dialog"
            className="flex min-h-[52px] w-full items-center justify-between rounded-[10px] bg-gray-50 px-5 py-[15px] text-left"
        >
            <span className="flex min-w-0 items-baseline gap-1">
                <span
                    className={`truncate text-[15px] ${station ? "font-medium text-gray-800" : "text-gray-500"
                        }`}
                >
                    {station?.name ?? placeholder}
                </span>
                {station ? (
                    <span className="shrink-0 text-xs text-gray-500">
                        {station.region}
                    </span>
                ) : null}
            </span>
            <ChevronDownIcon />
        </button>
    );
}

function EditableProfileField({
    label,
    value,
    placeholder,
    type = "text",
    inputMode,
    autoComplete,
    readOnly = false,
    onChange,
    onBlur,
}: {
    label: string;
    value: string;
    placeholder: string;
    type?: "text" | "tel" | "email";
    inputMode?: "text" | "tel" | "email";
    autoComplete?: string;
    readOnly?: boolean;
    onChange?: (value: string) => void;
    onBlur?: () => void;
}) {
    return (
        <label className="flex flex-col gap-1.5">
            <span className="text-sm font-medium leading-[22px] text-gray-600">
                {label}
            </span>
            <input
                type={type}
                inputMode={inputMode}
                value={value}
                placeholder={placeholder}
                autoComplete={autoComplete}
                readOnly={readOnly}
                onChange={(event) => onChange?.(event.target.value)}
                onBlur={onBlur}
                className={`${editableFieldClassName} ${readOnly
                    ? "cursor-default bg-gray-200 text-gray-500"
                    : "bg-gray-50 text-gray-800"
                    }`}
            />
        </label>
    );
}

export default function EditProfile() {
    const navigate = useNavigate();
    const currentUser = getCurrentUser();
    const pictureInputRef = useRef<HTMLInputElement>(null);
    const [nickname, setNickname] = useState("");
    const [name, setName] = useState("");
    const [phone, setPhone] = useState("");
    const [birthDate, setBirthDate] = useState("");
    const email = currentUser?.profileEmail || currentUser?.email || "";
    const [isNicknameEditing, setIsNicknameEditing] = useState(false);
    const [isDatePickerOpen, setIsDatePickerOpen] = useState(false);
    const [stationField, setStationField] = useState<StationField | null>(null);
    const [originStation, setOriginStation] = useState<Station | null>(null);
    const [destinationStation, setDestinationStation] =
        useState<Station | null>(null);
    const [wayPoints, setWayPoints] = useState<Station[]>([]);
    const [isSettingTransfers, setIsSettingTransfers] = useState(false);
    const [routeMessage, setRouteMessage] = useState("");
    const [routeHasError, setRouteHasError] = useState(false);
    const [isSaving, setIsSaving] = useState(false);
    const [saveMessage, setSaveMessage] = useState("");
    const [saveHasError, setSaveHasError] = useState(false);
    const [picturePreview, setPicturePreview] = useState<string | null>(null);
    const [pictureKey, setPictureKey] = useState<string | null>(null);
    const [isPictureUploading, setIsPictureUploading] = useState(false);
    const [pictureMessage, setPictureMessage] = useState("");
    const [pictureHasError, setPictureHasError] = useState(false);
    const loadProfile = useCallback(() => accountApi.getProfile(), []);
    const profileRequest = useApiRequest(loadProfile);

    useEffect(() => {
        void profileRequest.execute().catch(() => undefined);
    }, [profileRequest.execute]);

    useEffect(() => {
        if (!profileRequest.data) {
            return;
        }

        setNickname(profileRequest.data.nickname);
        setName(profileRequest.data.name);
        setPhone(formatPhoneNumber(profileRequest.data.phoneNumber));
        setBirthDate(profileRequest.data.birth);
        setOriginStation({
            id: profileRequest.data.startPlace.id,
            name: profileRequest.data.startPlace.stationName,
            region: profileRequest.data.startPlace.routeName,
        });
        setDestinationStation({
            id: profileRequest.data.destinationPlace.id,
            name: profileRequest.data.destinationPlace.stationName,
            region: profileRequest.data.destinationPlace.routeName,
        });
        setWayPoints(
            profileRequest.data.wayPoints.map((wayPoint) => ({
                id: wayPoint.id,
                name: wayPoint.stationName,
                region: wayPoint.routeName,
            })),
        );
    }, [profileRequest.data]);

    useEffect(() => {
        return () => {
            if (picturePreview) {
                URL.revokeObjectURL(picturePreview);
            }
        };
    }, [picturePreview]);

    const handlePictureChange = async (
        event: React.ChangeEvent<HTMLInputElement>,
    ) => {
        const file = event.target.files?.[0];
        event.target.value = "";

        if (!file) {
            return;
        }

        if (!file.type.startsWith("image/")) {
            setPictureHasError(true);
            setPictureMessage("이미지 파일만 선택할 수 있습니다.");
            return;
        }

        const nextPreview = URL.createObjectURL(file);
        setPicturePreview(nextPreview);
        setPictureKey(null);
        setIsPictureUploading(true);
        setPictureMessage("이미지를 업로드하는 중입니다...");
        setPictureHasError(false);

        try {
            const { imageKey, uploadUrl } = await fileApi.getImageUploadUrl({
                fileName: file.name,
                contentType: file.type || "application/octet-stream",
                fileSize: file.size,
            });
            await fileApi.uploadToPresignedUrl(uploadUrl, file);

            setPictureKey(imageKey);
            setPictureMessage(
                "이미지를 업로드했습니다. 변경사항을 저장해주세요.",
            );
        } catch (error) {
            setPicturePreview(null);
            setPictureKey(null);
            setPictureHasError(true);
            setPictureMessage(
                error instanceof ApiError
                    ? error.message
                    : "이미지를 업로드하지 못했습니다.",
            );
        } finally {
            setIsPictureUploading(false);
        }
    };

    const handleStationSelect = (station: Station) => {
        if (stationField === "origin") {
            setOriginStation(station);
        }

        if (stationField === "destination") {
            setDestinationStation(station);
        }

        if (
            stationField === "wayPoint" &&
            !wayPoints.some((wayPoint) => wayPoint.id === station.id)
        ) {
            setWayPoints((previous) => [...previous, station]);
        }

        setRouteMessage("");
        setRouteHasError(false);
        setStationField(null);
    };

    const handleAutoSetTransfers = async () => {
        if (!originStation || !destinationStation) {
            setRouteMessage("출발역과 도착역을 먼저 선택해주세요.");
            setRouteHasError(true);
            return;
        }

        if (originStation.id === destinationStation.id) {
            setRouteMessage("출발역과 도착역은 서로 달라야 합니다.");
            setRouteHasError(true);
            return;
        }

        setIsSettingTransfers(true);
        setRouteMessage("");
        setRouteHasError(false);

        try {
            const path = await subwayApi.path({
                originPlaceId: originStation.id,
                destinationPlaceId: destinationStation.id,
                waypointPlaceIds: wayPoints.map((wayPoint) => wayPoint.id),
            });
            const nextWayPoints = getRouteWayPoints(
                path.stations,
                wayPoints.map((wayPoint) => wayPoint.id),
            );

            setWayPoints(nextWayPoints);
            setRouteMessage(
                path.transferCount > 0
                    ? `환승역 ${path.transferCount}개를 자동으로 설정했습니다.`
                    : "환승이 필요 없는 경로입니다.",
            );
        } catch (error) {
            setRouteHasError(true);
            setRouteMessage(
                error instanceof ApiError
                    ? error.message
                    : "환승역을 자동 설정하지 못했습니다.",
            );
        } finally {
            setIsSettingTransfers(false);
        }
    };

    const handleSave = async () => {
        if (isPictureUploading) {
            setSaveHasError(true);
            setSaveMessage("이미지 업로드가 끝날 때까지 기다려주세요.");
            return;
        }

        if (!nickname.trim() || !name.trim() || !phone.trim() || !birthDate) {
            setSaveHasError(true);
            setSaveMessage("닉네임, 이름, 전화번호, 생년월일을 확인해주세요.");
            return;
        }

        if (!originStation || !destinationStation) {
            setSaveHasError(true);
            setSaveMessage("출발역과 도착역을 선택해주세요.");
            return;
        }

        const routeIds = [
            originStation.id,
            ...wayPoints.map((wayPoint) => wayPoint.id),
            destinationStation.id,
        ];
        if (new Set(routeIds).size !== routeIds.length) {
            setSaveHasError(true);
            setSaveMessage("출발역, 경유역, 도착역은 서로 달라야 합니다.");
            return;
        }

        setIsSaving(true);
        setSaveMessage("");
        setSaveHasError(false);

        try {
            await accountApi.editProfile({
                nickname: nickname.trim(),
                phoneNumber: phone,
                startPlaceId: originStation.id,
                destinationPlaceId: destinationStation.id,
                wayPoints: wayPoints.map((wayPoint) => wayPoint.id),
                name: name.trim(),
                birth: birthDate,
                ...(pictureKey ? { picture: pictureKey } : {}),
            });
            updateCurrentUserProfile({
                nickname: nickname.trim(),
                name,
                phone,
                birthDate,
            });
            setSaveMessage("프로필을 저장했습니다.");
            setTimeout(() => {
                navigate("/mypage", { replace: true });
            }, 1000);
        } catch (error) {
            setSaveHasError(true);
            setSaveMessage(
                error instanceof ApiError
                    ? error.message
                    : "프로필을 저장하지 못했습니다.",
            );
        } finally {
            setIsSaving(false);
        }
    };

    return (
        <main className="page-container relative flex h-full min-h-0 flex-col overflow-hidden">
            <PageHeader
                title="프로필 설정"
                onBack={() => navigate(-1)}
                className="shrink-0"
            />

            <div className="scrollbar-hidden min-h-0 flex-1 overflow-y-auto px-1 pb-6">
                {profileRequest.isLoading && !profileRequest.data ? (
                    <p className="py-6 text-center text-sm text-gray-500">
                        프로필을 불러오는 중입니다...
                    </p>
                ) : null}
                {profileRequest.error && !profileRequest.data ? (
                    <div
                        className="mt-5 rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-center"
                        role="alert"
                    >
                        <p className="text-sm text-red-600">
                            {profileRequest.error.message ||
                                "프로필을 불러오지 못했습니다."}
                        </p>
                        <button
                            type="button"
                            onClick={() =>
                                void profileRequest
                                    .execute()
                                    .catch(() => undefined)
                            }
                            className="mt-2 text-sm font-semibold text-red-600 underline"
                        >
                            다시 시도
                        </button>
                    </div>
                ) : null}

                <section className="flex flex-col items-center pb-8 pt-8">
                    <div className="relative">
                        <div className="flex h-[110px] w-[110px] items-center justify-center rounded-full bg-purple-100 text-3xl font-bold text-purple-700">
                            {picturePreview || profileRequest.data?.picture ? (
                                <img
                                    src={
                                        picturePreview ??
                                        (profileRequest.data?.picture
                                            ? imgproxied(
                                                profileRequest.data?.picture,
                                                110,
                                            )
                                            : "")
                                    }
                                    alt="프로필"
                                    className="h-full w-full rounded-full object-cover"
                                />
                            ) : (
                                (nickname || name || "?")
                                    .charAt(0)
                                    .toUpperCase()
                            )}
                        </div>
                        <button
                            type="button"
                            onClick={() => pictureInputRef.current?.click()}
                            disabled={isPictureUploading}
                            className="absolute bottom-0 right-0 flex h-8 w-8 items-center justify-center rounded-full bg-white shadow-md"
                            aria-label="프로필 사진 변경"
                            aria-busy={isPictureUploading}
                        >
                            <CameraIcon />
                        </button>
                        <input
                            ref={pictureInputRef}
                            type="file"
                            accept="image/*"
                            onChange={(event) =>
                                void handlePictureChange(event)
                            }
                            className="sr-only"
                            aria-label="프로필 이미지 선택"
                        />
                    </div>

                    {pictureMessage ? (
                        <p
                            className={`mt-3 text-xs ${pictureHasError
                                ? "text-red-500"
                                : "text-purple-600"
                                }`}
                            role={pictureHasError ? "alert" : "status"}
                        >
                            {pictureMessage}
                        </p>
                    ) : null}

                    <div
                        className={`${pictureMessage ? "mt-4" : "mt-8"} flex items-center gap-1`}
                    >
                        {isNicknameEditing ? (
                            <input
                                type="text"
                                value={nickname}
                                autoFocus
                                onChange={(event) =>
                                    setNickname(event.target.value)
                                }
                                onBlur={() => {
                                    setIsNicknameEditing(false);
                                }}
                                onKeyDown={(event) => {
                                    if (event.key === "Enter") {
                                        event.currentTarget.blur();
                                    }
                                }}
                                aria-label="닉네임"
                                className="w-40 border-b border-gray-300 bg-transparent text-center text-2xl font-bold leading-[30px] text-gray-900 outline-none"
                            />
                        ) : (
                            <h2 className="text-2xl font-bold leading-[30px] text-gray-900">
                                {nickname || "닉네임"}
                            </h2>
                        )}
                        <button
                            type="button"
                            onClick={() => setIsNicknameEditing(true)}
                            className="p-1 text-xl leading-none text-gray-500"
                            aria-label="닉네임 수정"
                        >
                            ✎
                        </button>
                    </div>
                </section>

                <div className="flex flex-col gap-5">
                    <EditableProfileField
                        label="이름"
                        value={name}
                        placeholder="이름을 입력해주세요"
                        autoComplete="name"
                        onChange={setName}
                    />
                    <EditableProfileField
                        label="전화번호"
                        value={phone}
                        placeholder="전화번호를 입력해주세요"
                        type="tel"
                        inputMode="tel"
                        autoComplete="tel"
                        onChange={(value) => setPhone(formatPhoneNumber(value))}
                    />
                    <BirthDateField
                        value={birthDate}
                        validationMessage=""
                        showValidation={false}
                        onOpen={() => setIsDatePickerOpen(true)}
                    />
                    <EditableProfileField
                        label="이메일"
                        value={email}
                        placeholder="이메일을 입력해주세요"
                        type="email"
                        inputMode="email"
                        autoComplete="email"
                        readOnly
                    />

                    <div className="flex flex-col gap-1.5">
                        <span className="text-sm font-medium leading-[22px] text-gray-600">
                            통학로 설정
                        </span>
                        <StationFieldButton
                            placeholder="출발지를 선택해주세요"
                            station={originStation}
                            onClick={() => setStationField("origin")}
                        />

                        {wayPoints.map((wayPoint) => (
                            <div
                                key={wayPoint.id}
                                className="flex min-h-[48px] items-center justify-between rounded-[10px] bg-purple-100 px-5 py-3"
                            >
                                <span className="flex min-w-0 items-baseline gap-1">
                                    <span className="truncate text-[15px] font-medium text-gray-800">
                                        {wayPoint.name}
                                    </span>
                                    <span className="shrink-0 text-xs text-gray-600">
                                        {wayPoint.region}
                                    </span>
                                </span>
                                <button
                                    type="button"
                                    onClick={() =>
                                        setWayPoints((previous) =>
                                            previous.filter(
                                                (station) =>
                                                    station.id !== wayPoint.id,
                                            ),
                                        )
                                    }
                                    className="text-xs font-semibold text-gray-600"
                                    aria-label={`${wayPoint.name} 경유지 삭제`}
                                >
                                    삭제
                                </button>
                            </div>
                        ))}

                        <StationFieldButton
                            placeholder="도착지를 선택해주세요"
                            station={destinationStation}
                            onClick={() => setStationField("destination")}
                        />

                        {routeMessage ? (
                            <p
                                className={`px-1 text-xs ${routeHasError
                                    ? "text-red-500"
                                    : "text-purple-600"
                                    }`}
                                role={routeHasError ? "alert" : "status"}
                            >
                                {routeMessage}
                            </p>
                        ) : null}
                    </div>

                    <div className="my-3 flex items-center justify-center gap-[10px]">
                        <button
                            type="button"
                            onClick={() => setStationField("wayPoint")}
                            className="w-[100px] text-right text-[13px] font-semibold text-gray-500"
                        >
                            경유지 추가
                        </button>
                        <span className="w-2 text-[13px] font-semibold text-gray-200">
                            |
                        </span>
                        <button
                            type="button"
                            onClick={handleAutoSetTransfers}
                            disabled={isSettingTransfers}
                            className="w-[100px] text-left text-[13px] font-semibold text-purple-500 disabled:cursor-wait disabled:text-gray-400"
                        >
                            {isSettingTransfers
                                ? "경로 탐색 중..."
                                : "환승역 자동설정"}
                        </button>
                    </div>

                    {/* <div className="flex flex-col gap-1.5">
                        <span className="text-sm font-medium leading-[22px] text-gray-600">
                            비밀번호 변경
                        </span>
                        <button
                            type="button"
                            onClick={() => navigate("/mypage/edit/password")}
                            className="flex min-h-[52px] w-full items-center justify-between rounded-[10px] bg-gray-50 px-5 py-[15px] text-left transition-colors hover:bg-gray-100"
                        >
                            <span className="text-base font-medium leading-[22px] text-gray-700">
                                비밀번호 변경
                            </span>
                            <MoveIcon />
                        </button>
                    </div> */}
                </div>
            </div>

            <div className="shrink-0 pt-3">
                {saveMessage ? (
                    <p
                        className={`mb-2 text-center text-xs ${saveHasError ? "text-red-500" : "text-purple-600"
                            }`}
                        role={saveHasError ? "alert" : "status"}
                    >
                        {saveMessage}
                    </p>
                ) : null}
                <button
                    type="button"
                    onClick={() => void handleSave()}
                    disabled={
                        !originStation ||
                        !destinationStation ||
                        profileRequest.isLoading ||
                        Boolean(profileRequest.error) ||
                        isSaving ||
                        isSettingTransfers ||
                        isPictureUploading
                    }
                    className="w-full rounded-lg bg-purple-500 py-3.5 font-bold text-white transition-colors hover:bg-purple-600 disabled:cursor-not-allowed disabled:bg-gray-200 disabled:text-gray-400"
                >
                    {isSaving ? "저장 중..." : "변경사항 저장"}
                </button>

                <div className="w-full flex justify-center">
                    <span
                        className="text-sm font-medium leading-[22px] text-gray-600 mt-2 mb-4 cursor-pointer"
                        onClick={() => {
                            navigate("/mypage/edit/password");
                        }}
                    >
                        비밀번호 변경
                    </span>
                </div>
            </div>

            {isDatePickerOpen ? (
                <DatePickerSheet
                    value={birthDate}
                    onClose={() => setIsDatePickerOpen(false)}
                    onConfirm={(value) => {
                        setBirthDate(value);
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
        </main>
    );
}
