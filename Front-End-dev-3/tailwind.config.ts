import type { Config } from "tailwindcss";

export default {
    content: ["./index.html", "./src/**/*.{ts,tsx}"],
    theme: {
        extend: {
            colors: {
                purple: {
                    100: "#DEE0FF",
                    200: "#C6C8FF",
                    300: "#A9ACFF",
                    400: "#8488FF",
                    500: "#6E73F5",
                    600: "#5A60F9",
                    700: "#4146D0",
                    800: "#3A3E9A",
                    900: "#2A2D86",
                },
                gray: {
                    50: "#F8F9FD",
                    100: "#E6E8F1",
                    200: "#DADCE6",
                    300: "#C6C8D2",
                    400: "#B3B5C1",
                    500: "#8E91A1",
                    600: "#70727E",
                    700: "#51525C",
                    800: "#373840",
                    900: "#1D1E23",
                },
                errorRed: "#FF3D3D",
            },
        },
    },
    plugins: [],
} satisfies Config;
