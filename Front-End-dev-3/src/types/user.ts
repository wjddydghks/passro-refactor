export type UserRole = "sender" | "shipper";

export type SchoolVerificationStatus =
    | "verified"
    | "pending"
    | "rejected"
    | "none";

export interface UserProfile {
    id: string;
    name: string;
    email: string;
    schoolName: string;
    department?: string;
    avatarUrl?: string;
    role: UserRole;
    verificationStatus: SchoolVerificationStatus;
    rating: number;
    reviewCount: number;
    pointBalance: number;
    joinedAt?: string;
}

export interface CommuteRouteSummary {
    id: string;
    name: string;
    origin: string;
    destination: string;
    timeRange: string;
}

export interface ProfileStats {
    deliveryRequests: number;
    completedDeliveries: number;
    savedRoutes: number;
    acceptanceRate?: number;
}

export interface ProfilePageData {
    profile: UserProfile;
    primaryRoute?: CommuteRouteSummary;
    stats: ProfileStats;
}

export type ProfilePlace = {
    id: number;
    region: string;
    routeName: string;
    stationName: string;
    latitude: number;
    longitude: number;
};

export type Profile = {
    picture?: string;
    nickname: string;
    name: string;
    birth: string;
    phoneNumber: string;
    deliveryCount: number;
    point: number;
    rating: number;
    createdAt: string;
    startPlace: ProfilePlace;
    destinationPlace: ProfilePlace;
    wayPoints: ProfilePlace[];
};

export type EditProfile = {
    nickname: string;
    phoneNumber: string;
    startPlaceId: number;
    destinationPlaceId: number;
    wayPoints?: number[];
    name: string;
    birth: string;
    picture?: string;
};

export type EditPassword = {
    password: string;
    code: string;
};
