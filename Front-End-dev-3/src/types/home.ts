export type ActiveDelivery = {
  id: number;
  title: string;
  route: string;
  status: string;
};

export type MatchingRequest = {
  id: number;
  title: string;
  route: string;
  timeLeft: string;
};

export type RecentHistory = {
  id: number;
  title: string;
  route: string;
  status: string;
};

export type HomeContent = {
  name: string;
  headline: string;
  activeDeliveries: ActiveDelivery[];
  matchingRequests: MatchingRequest[];
  recentHistories: RecentHistory[];
  actionLabel?: string;
};
