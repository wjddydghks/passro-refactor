import type { UserRole } from "../types/user";
import { accountApi } from "../apis/accountApi";
import { authApi } from "../apis/authApi";
import { tokenStorage } from "../apis/tokenStorage";

const AUTH_USER_KEY = "passro.authUser";
const SELECTED_USER_ROLE_KEY = "passro.selectedUserRole";

export interface AuthUser {
    id: string;
    email: string;
    name: string;
    role: UserRole;
    nickname: string;
    phone?: string;
    birthDate?: string;
    address?: string;
    profileEmail?: string;
}

function createUserId(email: string) {
    return email.trim().toLowerCase();
}

function getNameFromEmail(email: string) {
    const localPart = email.split("@")[0]?.trim();
    return localPart || "패스로 사용자";
}

function getRoleFromEmail(email: string): UserRole {
    const normalizedEmail = email.toLowerCase();
    return normalizedEmail.includes("shipper") ||
        normalizedEmail.includes("delivery")
        ? "shipper"
        : "sender";
}

function isUserRole(value: string | null): value is UserRole {
    return value === "sender" || value === "shipper";
}

function createSessionUser(email: string, nickname = ""): AuthUser {
  const normalizedEmail = email.trim().toLowerCase();
  const authUser: AuthUser = {
    id: createUserId(normalizedEmail),
    email: normalizedEmail,
    name: getNameFromEmail(normalizedEmail),
    role: getRoleFromEmail(normalizedEmail),
    nickname,
  };

  localStorage.setItem(AUTH_USER_KEY, JSON.stringify(authUser));
  return authUser;
}

function clearLocalSession() {
  localStorage.removeItem(AUTH_USER_KEY);
  localStorage.removeItem(SELECTED_USER_ROLE_KEY);
  tokenStorage.clearTokens();
}

export async function login(email: string, password: string): Promise<AuthUser> {
  const tokens = await authApi.login({
    mail: email.trim(),
    password,
  });

  tokenStorage.setTokens(tokens.accessToken, tokens.refreshToken);
  const sessionUser = createSessionUser(email);

  try {
    const profile = await accountApi.getProfile();

    return updateCurrentUserProfile({
      name: profile.name,
      nickname: profile.nickname,
      phone: profile.phoneNumber,
      birthDate: profile.birth,
    }) ?? sessionUser;
  } catch {
    return sessionUser;
  }
}

export async function logout(): Promise<void> {
  try {
    if (tokenStorage.getAccessToken()) {
      await authApi.logout();
    }
  } catch {
    // 서버 로그아웃 실패와 관계없이 로컬 인증 정보는 반드시 제거한다.
  } finally {
    clearLocalSession();
  }
}

export function getCurrentUser(): AuthUser | null {
  const storedUser = localStorage.getItem(AUTH_USER_KEY);

  if (!storedUser) {
    return null;
  }

  try {
    return JSON.parse(storedUser) as AuthUser;
  } catch {
    clearLocalSession();
    return null;
  }
}

export function isAuthenticated() {
  return tokenStorage.getAccessToken() !== null && getCurrentUser() !== null;
}

export function getSelectedUserRole(): UserRole | null {
    const storedRole = localStorage.getItem(SELECTED_USER_ROLE_KEY);
    return isUserRole(storedRole) ? storedRole : null;
}

export function setCurrentUserRole(role: UserRole) {
    localStorage.setItem(SELECTED_USER_ROLE_KEY, role);

    const currentUser = getCurrentUser();
    if (!currentUser) {
        return null;
    }

    const updatedUser: AuthUser = {
        ...currentUser,
        role,
    };

    localStorage.setItem(AUTH_USER_KEY, JSON.stringify(updatedUser));
    return updatedUser;
}

export function updateCurrentUserProfile(
    updates: Partial<
        Pick<
            AuthUser,
            | "name"
            | "nickname"
            | "phone"
            | "birthDate"
            | "address"
            | "profileEmail"
        >
    >,
) {
    const currentUser = getCurrentUser();

    if (!currentUser) {
        return null;
    }

    const updatedUser: AuthUser = {
        ...currentUser,
        ...updates,
    };

    localStorage.setItem(AUTH_USER_KEY, JSON.stringify(updatedUser));
    return updatedUser;
}
