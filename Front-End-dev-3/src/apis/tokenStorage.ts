const ACCESS_TOKEN_KEY = "passro.accessToken";
const REFRESH_TOKEN_KEY = "passro.refreshToken";

export const tokenStorage = {
    getAccessToken() {
        return localStorage.getItem(ACCESS_TOKEN_KEY);
    },

    getRefreshToken() {
        return localStorage.getItem(REFRESH_TOKEN_KEY);
    },

    setAccessToken(token: string) {
        localStorage.setItem(ACCESS_TOKEN_KEY, token);
    },

    setRefreshToken(token: string) {
        localStorage.setItem(REFRESH_TOKEN_KEY, token);
    },

    setTokens(accessToken: string, refreshToken: string) {
        localStorage.setItem(ACCESS_TOKEN_KEY, accessToken);
        localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken);
    },

    clearAccessToken() {
        localStorage.removeItem(ACCESS_TOKEN_KEY);
    },

    clearTokens() {
        localStorage.removeItem(ACCESS_TOKEN_KEY);
        localStorage.removeItem(REFRESH_TOKEN_KEY);
    },

    clearTokensIfRefreshTokenMatches(refreshToken: string) {
        if (localStorage.getItem(REFRESH_TOKEN_KEY) !== refreshToken) {
            return false;
        }

        localStorage.removeItem(ACCESS_TOKEN_KEY);
        localStorage.removeItem(REFRESH_TOKEN_KEY);
        return true;
    },
};
