import { useCallback, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import ProfilePage from "../components/profile/ProfilePage";
import { getCurrentUser, logout } from "../utils/auth";
import { accountApi, senderDeliveryApi, shipperDeliveryApi } from "../apis";
import { useApiRequest } from "../hooks/useApiRequest";

export default function MyPage() {
    const navigate = useNavigate();
    const currentUser = getCurrentUser()!;

    const loadProfile = useCallback(() => accountApi.getProfile(), []);
    const profileRequest = useApiRequest(loadProfile);

    useEffect(() => {
        void profileRequest.execute().catch(() => undefined);
    }, [profileRequest.execute]);

    const loadDeliveryCounts = useCallback(
        () =>
            Promise.all([
                senderDeliveryApi.getDeliveryList(),
                shipperDeliveryApi.getDeliveryList(),
            ]),
        [],
    );
    const deliveryCountRequest = useApiRequest(loadDeliveryCounts);

    useEffect(() => {
        void deliveryCountRequest.execute().catch(() => undefined);
    }, [deliveryCountRequest.execute]);

    const activityCount =
        (deliveryCountRequest.data?.[0]?.length ?? 0) +
        (deliveryCountRequest.data?.[1]?.length ?? 0);

    const handleEditProfile = useCallback(() => {
        navigate("/mypage/edit");
    }, [navigate]);

    const handleInquiry = useCallback(() => {
        navigate("/mypage/inquiry");
    }, [navigate]);

    const handleBack = useCallback(() => {
        navigate("/home");
    }, [navigate]);

    const handleViewPoints = useCallback(() => {
        navigate("/mypage/point");
    }, [navigate]);

    const handleViewHistory = useCallback(() => {
        navigate("/mypage/history");
    }, [navigate]);

    const handleLogout = useCallback(async () => {
        await logout();
        navigate("/login", { replace: true });
    }, [navigate]);

    return (
        <ProfilePage
            profile={profileRequest.data}
            role={currentUser.role}
            averageRating={profileRequest.data?.rating ?? 0}
            activityCount={activityCount}
            isLoading={profileRequest.isLoading}
            error={profileRequest.error}
            onRetry={() => void profileRequest.execute().catch(() => undefined)}
            onBack={handleBack}
            onEditProfile={handleEditProfile}
            onInquiry={handleInquiry}
            onViewPoints={handleViewPoints}
            onViewHistory={handleViewHistory}
            onLogout={handleLogout}
        />
    );
}
