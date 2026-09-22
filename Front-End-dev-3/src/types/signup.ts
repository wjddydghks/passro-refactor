export type SignupStep = "basic" | "detail";

export type SignupGender = "M" | "W" | "NONE";

export type SignupNicknameCheckStatus = "idle" | "available" | "duplicate";
export type SignupEmailVerificationStatus = "idle" | "sent" | "verified";

export type SignupDateValue = {
  year: number;
  month: number;
  day: number;
};

export type SignupStation = {
  id: number;
  name: string;
  region: string;
};

export type SignupRouteStations = {
  origin: SignupStation | null;
  destination: SignupStation | null;
  wayPoints: SignupStation[];
};

export type SignupDetailValidationMessages = {
  name: string;
  phone: string;
  birthDate: string;
  originStation: string;
  destinationStation: string;
};

export type SignupFormData = {
  nickname: string;
  email: string;
  emailCode: string;
  password: string;
  passwordCheck: string;
  name: string;
  phone: string;
  birthDate: string;
  gender: SignupGender;
  address: string;
  originStationId: number;
  destinationStationId: number;
  wayPoints: Array<number>;
};

export type SignupFieldUpdater = <K extends keyof SignupFormData>(
  key: K,
  value: SignupFormData[K],
) => void;
