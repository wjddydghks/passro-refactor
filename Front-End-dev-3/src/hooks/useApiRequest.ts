import { useCallback, useState } from "react";
import { ApiError } from "../types/api";

export function useApiRequest<TArgs extends unknown[], TResult>(
    request: (...args: TArgs) => Promise<TResult>,
) {
    const [data, setData] = useState<TResult | null>(null);
    const [error, setError] = useState<ApiError | null>(null);
    const [isLoading, setIsLoading] = useState(false);

    const execute = useCallback(
        async (...args: TArgs) => {
            setIsLoading(true);
            setError(null);

            try {
                const result = await request(...args);
                setData(result);
                return result;
            } catch (caughtError) {
                const apiError =
                    caughtError instanceof ApiError
                        ? caughtError
                        : new ApiError({
                              message:
                                  caughtError instanceof Error
                                      ? caughtError.message
                                      : "알 수 없는 오류가 발생했습니다.",
                          });
                setError(apiError);
                throw apiError;
            } finally {
                setIsLoading(false);
            }
        },
        [request],
    );

    const reset = useCallback(() => {
        setData(null);
        setError(null);
        setIsLoading(false);
    }, []);

    return {
        data,
        error,
        isLoading,
        execute,
        reset,
    };
}
