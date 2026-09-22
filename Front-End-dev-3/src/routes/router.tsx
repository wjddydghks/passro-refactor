import { lazy } from "react";
import { createBrowserRouter, Navigate } from "react-router-dom";
import App from "../App";
import ProtectedRoute from "../components/common/ProtectedRoute";
import MainLayout from "../layouts/MainLayout";
import NotFoundPage from "../pages/NotFoundPage";
import RouteErrorPage from "../pages/RouteErrorPage";
import FeedbackPage from "../pages/FeedbackPage";
import ReportPage from "../pages/ReportPage";

const LoginPage = lazy(() => import("../pages/LoginPage"));
const SignupPage = lazy(() => import("../pages/SignupPage"));
const FindIdPage = lazy(() => import("../pages/FindIdPage"));
const FindPasswordPage = lazy(() => import("../pages/FindPasswordPage"));
const UserStateChoice = lazy(() => import("../pages/UserStateChoice"));
const HomePage = lazy(() => import("../pages/HomePage"));
const MarketPage = lazy(() => import("../pages/MarketPage"));
const MyPage = lazy(() => import("../pages/MyPage"));
const EditProfile = lazy(() => import("../pages/EditProfile"));
const InquiryPage = lazy(() => import("../pages/InquiryPage"));
const ChangePasswordPage = lazy(() => import("../pages/ChangePasswordPage"));
const PointPage = lazy(() => import("../pages/PointPage"));
const HistoryStatsPage = lazy(() =>
    import("../pages/HistoryStatsPage").then((module) => ({
        default: module.HistoryStatsPage,
    })),
);
const DeliveryMatchingPage = lazy(
    () => import("../pages/DeliveryMatchingPage"),
);
const DeliveryRequestPage = lazy(() => import("../pages/DeliveryRequestPage"));
const RequestCompletePage = lazy(() => import("../pages/RequestCompletePage"));
const DeliveryTrackingPage = lazy(
    () => import("../pages/DeliveryTrackingPage"),
);
const DeliveryStatusPage = lazy(() => import("../pages/DeliveryStatusPage"));
const ChatListPage = lazy(() => import("../pages/ChatListPage"));
const ChatPage = lazy(() => import("../pages/ChatPage"));

export const router = createBrowserRouter([
    {
        path: "/",
        element: <App />,
        errorElement: <RouteErrorPage />,
        children: [
            {
                index: true,
                element: <Navigate to="/login" replace />,
            },
            {
                path: "login",
                element: <LoginPage />,
            },
            {
                path: "user-state-choice",
                element: <UserStateChoice />,
            },
            {
                path: "signup",
                element: <SignupPage />,
            },
            {
                path: "find-id",
                element: <FindIdPage />,
            },
            {
                path: "find-password",
                element: <FindPasswordPage />,
            },
            {
                path: "find-pwd",
                element: <Navigate to="/find-password" replace />,
            },
            {
                path: "*",
                element: <NotFoundPage />,
            },
        ],
    },
    {
        element: <ProtectedRoute />,
        errorElement: <RouteErrorPage />,
        children: [
            {
                element: <MainLayout />,
                children: [
                    {
                        path: "/home",
                        element: <HomePage />,
                    },
                    {
                        path: "/market",
                        element: <MarketPage />,
                    },
                    {
                        path: "/mypage",
                        children: [
                            { index: true, element: <MyPage /> },
                            { path: "edit", element: <EditProfile /> },
                            { path: "inquiry", element: <InquiryPage /> },
                            {
                                path: "edit/password",
                                element: <ChangePasswordPage />,
                            },
                            { path: "point", element: <PointPage /> },
                            {
                                path: "history",
                                element: <HistoryStatsPage />,
                            },
                        ],
                    },
                    {
                        path: "report",
                        element: <ReportPage />,
                    },
                    {
                        path: "/delivery",
                        children: [
                            {
                                path: "matching",
                                element: <Navigate to="/home" replace />,
                            },
                            {
                                path: "matching/:deliveryId",
                                element: <DeliveryMatchingPage />,
                            },
                            {
                                path: "request",
                                element: <DeliveryRequestPage />,
                            },
                            {
                                path: "request/complete",
                                element: <RequestCompletePage />,
                            },
                            {
                                path: "tracking",
                                element: <Navigate to="/home" replace />,
                            },
                            {
                                path: "tracking/:deliveryId",
                                element: <DeliveryTrackingPage />,
                            },
                            {
                                path: "status",
                                element: <Navigate to="/home" replace />,
                            },
                            {
                                path: "status/:deliveryId",
                                element: <DeliveryStatusPage />,
                            },
                            {
                                path: "chat",
                                element: <ChatListPage />,
                            },
                            {
                                path: "chat/:chatRoomId",
                                element: <ChatPage />,
                            },
                            {
                                path: "feedback/:deliveryId",
                                element: <FeedbackPage />,
                            },
                        ],
                    },
                ],
            },
        ],
    },
]);
